@echo off
REM Smart GCC wrapper for Windows ARM64 cross builds
REM Routes Fortran files to flang, others to clang

setlocal ENABLEDELAYEDEXPANSION

REM Detect Fortran sources in arguments
set IS_FORTRAN=0

for %%A in (%*) do (
    set EXT=%%~xA
    if /I "!EXT!"==".f" set IS_FORTRAN=1
    if /I "!EXT!"==".for" set IS_FORTRAN=1
    if /I "!EXT!"==".f90" set IS_FORTRAN=1
)


if "%IS_FORTRAN%"=="1" (
    REM Fortran
    call flang-new %*
) else (
    REM C / C++
    call clang %*
)

endlocal
