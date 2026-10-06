@echo off
echo ===================================================
echo Resetting MySQL root password to 'root'...
echo ===================================================
net stop MySQL80
echo ALTER USER 'root'@'localhost' IDENTIFIED BY 'root'; > "%TEMP%\reset_mysql.txt"
echo FLUSH PRIVILEGES; >> "%TEMP%\reset_mysql.txt"
start "" /B "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe" --defaults-file="C:\ProgramData\MySQL\MySQL Server 8.0\my.ini" --init-file="%TEMP%\reset_mysql.txt"
timeout /t 6 /nobreak >nul
taskkill /F /IM mysqld.exe >nul 2>&1
timeout /t 2 /nobreak >nul
net start MySQL80
del "%TEMP%\reset_mysql.txt" 2>nul
echo ===================================================
echo SUCCESS! MySQL root password is now: root
echo ===================================================
pause
