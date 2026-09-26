<#
.SYNOPSIS
    Compila y prueba LUMA, y deja un reporte corto que Claude puede leer solo.

.DESCRIPTION
    Existe por una limitacion concreta: el entorno de Claude no alcanza Maven
    Central ni Docker Hub, asi que no puede compilar. Este script cierra ese
    hueco sin copiar y pegar nada.

    Genera dos archivos en la raiz del proyecto:

      build-report.txt   Solo lo que importa: errores, pruebas y resultado.
      build-full.log     La salida completa, por si el reporte no basta.

    Ambos estan en .gitignore. Despues de correrlo basta decir "listo" en el
    chat: Claude lee el reporte por el puente con tu computadora.

.PARAMETER Target
    backend  (por omision)  Compila y corre las pruebas del backend.
    frontend                Verifica tipos y compila el frontend.
    all                     Los dos.

.PARAMETER SkipTests
    Solo compila. Util cuando persigues un error de compilacion.

.EXAMPLE
    .\scripts\verify.ps1
    .\scripts\verify.ps1 -Target all
    .\scripts\verify.ps1 -SkipTests
#>

[CmdletBinding()]
param(
    [ValidateSet('backend', 'frontend', 'all')]
    [string]$Target = 'backend',

    [switch]$SkipTests
)

$ErrorActionPreference = 'Continue'

$root = Split-Path -Parent $PSScriptRoot
$log = Join-Path $root 'build-full.log'
$report = Join-Path $root 'build-report.txt'

# Misma imagen que usa backend/Dockerfile: si el proyecto levanta, esta existe.
$mavenImage = 'maven:3.9-eclipse-temurin-25'
$nodeImage = 'node:24-alpine'

# El repositorio local de Maven se monta para que la segunda corrida no vuelva
# a descargar nada. Sin esto cada verificacion tarda varios minutos.
$m2 = Join-Path $env:USERPROFILE '.m2'
if (-not (Test-Path $m2)) { New-Item -ItemType Directory -Path $m2 | Out-Null }

"LUMA - verificacion $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')" |
    Out-File -FilePath $log -Encoding utf8

# Todo lo que entra al log pasa por aqui, y siempre en UTF-8.
#
# Es la correccion de un defecto real: `Tee-Object -FilePath` en PowerShell 5.1
# escribe UTF-16LE y no acepta parametro de codificacion. Mezclado con un
# encabezado en UTF-8, el archivo quedaba con dos codificaciones y Select-String
# no encontraba ni una sola linea: el reporte salia vacio aunque el build
# estuviera bien.
function Write-Log([string[]]$lines) {
    $lines | Out-File -FilePath $log -Append -Encoding utf8
}

function Write-Section([string]$title) {
    $line = "`n===== $title =====`n"
    Write-Host $line -ForegroundColor Cyan
    Write-Log $line
}

function Invoke-Step([string]$title, [string[]]$dockerArgs) {
    Write-Section $title

    # Tee-Object hacia una VARIABLE, no hacia el archivo: asi la salida se ve en
    # vivo en la consola (Out-Host) y el log lo escribe Write-Log con una sola
    # codificacion. Sin el Out-Host final, cada linea saldria por la tuberia y el
    # valor de retorno de la funcion seria el log entero en vez del codigo.
    & docker @dockerArgs 2>&1 | Tee-Object -Variable stepOut | Out-Host
    $code = $LASTEXITCODE

    Write-Log ($stepOut | ForEach-Object { $_.ToString() })
    return $code
}

# Falla temprano y con un mensaje claro si Docker no esta corriendo. El error
# nativo de docker en ese caso no dice nada util.
& docker info 2>&1 | Out-Null
if ($LASTEXITCODE -ne 0) {
    Write-Host "Docker no responde. Abre Docker Desktop y espera a que el motor" -ForegroundColor Red
    Write-Host "aparezca como 'running', luego vuelve a correr este script." -ForegroundColor Red
    exit 1
}

$exitCode = 0

if ($Target -in @('backend', 'all')) {
    $goal = if ($SkipTests) { 'test-compile' } else { 'test' }

    # Por que se monta el socket de Docker
    # ------------------------------------
    # Maven corre DENTRO de un contenedor, y LumaApplicationTests usa
    # Testcontainers para levantar un MySQL real. Sin el socket, Testcontainers
    # no encuentra ningun demonio de Docker al cual pedirselo y falla con
    # "Could not find a valid Docker environment".
    #
    # Con el socket montado, el MySQL que arranca Testcontainers no es un
    # contenedor hijo: es un HERMANO que vive en tu demonio de Docker, al lado
    # del de Maven. Por eso hace falta TESTCONTAINERS_HOST_OVERRIDE: sin el,
    # Testcontainers le pasaria a Spring una URL con "localhost", y localhost
    # dentro del contenedor de Maven no es tu maquina.
    #
    # ADVERTENCIA: montar el socket le da a ese contenedor control del demonio
    # de Docker de tu equipo. Es la practica habitual para correr Testcontainers
    # asi, y aqui el contenedor solo ejecuta tu propio codigo, pero conviene
    # saberlo y no copiar este patron a ciegas en otros proyectos.
    $code = Invoke-Step "BACKEND ($goal)" @(
        'run', '--rm',
        '-v', "${root}\backend:/app",
        '-v', "${m2}:/root/.m2",
        '-v', '/var/run/docker.sock:/var/run/docker.sock',
        '-e', 'TESTCONTAINERS_HOST_OVERRIDE=host.docker.internal',
        '-w', '/app',
        $mavenImage,
        # `clean` no es opcional aqui. Sin el, Maven responde "Nothing to compile
        # - all classes are up to date" y corre las pruebas contra los .class de
        # una compilacion anterior. Un script cuyo unico trabajo es verificar no
        # puede dar por bueno un resultado que quiza no venga del codigo actual.
        # Cuesta unos segundos mas y los vale.
        'mvn', '-B', 'clean', $goal
    )
    if ($code -ne 0) { $exitCode = $code }
}

if ($Target -in @('frontend', 'all')) {
    # El volumen anonimo sobre /app/node_modules NO es opcional.
    #
    # La carpeta frontend esta montada desde Windows, asi que sin esta linea el
    # `npm ci` de adentro instalaria binarios nativos de Linux ENCIMA de los de
    # Windows y te dejaria `npm run dev` roto hasta reinstalar. Con ella, el
    # contenedor usa sus propias dependencias y las tuyas quedan intactas.
    $code = Invoke-Step 'FRONTEND (formato, lint, tipos, pruebas y build)' @(
        'run', '--rm',
        '-v', "${root}\frontend:/app",
        '-v', '/app/node_modules',
        '-w', '/app',
        $nodeImage,
        'sh', '-c',
        'npm ci --no-audit --no-fund && npm run format:check && npm run lint && npm run build && npm run test'
    )
    if ($code -ne 0) { $exitCode = $code }
}

# ---------------------------------------------------------------------------
# El reporte corto
# ---------------------------------------------------------------------------
# Se filtra a proposito: el log completo trae cientos de lineas de descarga que
# no dicen nada. Lo que importa son los errores de compilacion, las pruebas que
# fallaron y el veredicto.
# Los del backend salen de Maven; los del frontend, de prettier, eslint, vite y
# vitest. Se evitan los simbolos (marcas de exito, cruces) a proposito: la
# consola de Windows los escribe con otra codificacion y no harian match.
$patterns = @(
    # --- Backend
    '^\[ERROR\]',
    '^\[FATAL\]',
    'Tests run:',
    'BUILD SUCCESS',
    'BUILD FAILURE',
    '^\s+at com\.luma\.',
    'COMPILATION ERROR',
    'Caused by:',
    # --- Frontend
    'error TS\d+',
    'Prettier code style',
    'Code style issues',
    'problems \(',
    'built in ',
    'Test Files',
    '^\s*Tests\s+\d+',
    'Some chunks are larger',
    'npm warn deprecated',
    'npm ERR!',
    # --- Secciones del propio script
    '^===== '
)

$lines = Select-String -Path $log -Pattern ($patterns -join '|') |
    ForEach-Object { $_.Line.TrimEnd() } |
    # Las clases que pasaron no aportan nada al reporte: son 30 lineas de ruido.
    # Se quedan el total y cualquier clase con fallos.
    Where-Object { -not ($_ -match 'Tests run:.*Failures: 0, Errors: 0.*-- in ') }

$header = @(
    "LUMA - reporte de verificacion",
    "Fecha    : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')",
    "Objetivo : $Target$(if ($SkipTests) { ' (sin pruebas)' })",
    "Resultado: $(if ($exitCode -eq 0) { 'OK' } else { "FALLO (codigo $exitCode)" })",
    "Log completo: build-full.log",
    ""
)

Set-Content -Path $report -Value ($header + $lines) -Encoding utf8

Write-Host ""
if ($exitCode -eq 0) {
    Write-Host "OK. Reporte en build-report.txt" -ForegroundColor Green
} else {
    Write-Host "Fallo (codigo $exitCode). Reporte en build-report.txt" -ForegroundColor Red
}
Write-Host "Di 'listo' en el chat y Claude lo lee." -ForegroundColor Yellow

exit $exitCode
