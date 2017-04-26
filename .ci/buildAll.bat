@echo off

setlocal enabledelayedexpansion

set PWD=%cd%

for /d %%d in (%PWD%\*) do (
    if exist "%%d\gradlew.bat" (
        cd /d %%d
        call gradlew.bat build
        set ERRORCODE=!ERRORLEVEL!
        if !ERRORCODE! neq 0 (
            goto QUIT
        )
    )
)

:QUIT

cd /d %PWD%

exit /b !ERRORCODE!
