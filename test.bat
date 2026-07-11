@echo off
echo Compiling source and tests...
if not exist bin mkdir bin
javac -cp "lib/*" src/api/*.java src/db/*.java src/exceptions/*.java src/models/*.java src/service/*.java src/utils/*.java src/tests/*.java -d bin
if %ERRORLEVEL% NEQ 0 (
    echo Compilation failed!
    exit /b %ERRORLEVEL%
)
echo Running unit tests...
java -jar lib/junit-platform-console-standalone-1.10.0.jar -cp bin --scan-class-path
