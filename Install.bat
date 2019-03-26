@echo off 
rem BLIS Interface Client Startup Installation
set BLISDIR=%cd%
cd "%userprofile%\Start Menu\Programs\Startup" 
echo cd "%BLISDIR%" > "BLISInterfaceClient.bat" 
echo start BLISInterfaceClient.jar >> "BLISInterfaceClient.bat" 
echo Done!
pause
exit