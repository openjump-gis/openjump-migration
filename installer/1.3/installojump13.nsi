; Script generated by the HM NIS Edit Script Wizard.

; HM NIS Edit Wizard helper defines
!define PRODUCT_NAME "OpenJUMP"
!define PRODUCT_VERSION "1.3"
!define PRODUCT_PUBLISHER "Jump Pilot Project"
!define PRODUCT_WEB_SITE "http://www.openjump.org"
!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\openjump.exe"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"
  
!define JRE_VERSION "1.5"
!define JRE_URL "http://dlc.sun.com/jdk/jre-1_5_0_01-windows-i586-p.exe"

; MUI 1.67 compatible ------
!include "MUI.nsh"

; MUI Settings
!define MUI_ABORTWARNING
!define MUI_WELCOMEFINISHPAGE_BITMAP side_left.bmp
!define MUI_ICON "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
!define MUI_UNICON "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"
  
; Welcome page
!insertmacro MUI_PAGE_WELCOME
; License page
!insertmacro MUI_PAGE_LICENSE "openjump-1.3forwin\gpl2_license.txt"
; Directory page
!insertmacro MUI_PAGE_DIRECTORY
; Instfiles page
!insertmacro MUI_PAGE_INSTFILES
; Finish page
!define MUI_FINISHPAGE_RUN "$INSTDIR\bin\openjump.exe"
!insertmacro MUI_PAGE_FINISH

; Uninstaller pages
!insertmacro MUI_UNPAGE_INSTFILES

; Language files
!insertmacro MUI_LANGUAGE "English"

; MUI end ------

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "Setup-openjump13.exe"
InstallDir "$PROGRAMFILES\OpenJUMP"
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""
ShowInstDetails show
ShowUnInstDetails show

Section "Hauptgruppe" SEC01
  Call DetectJRE
  SetOutPath "$INSTDIR\bin\"
  SetOverwrite ifnewer
  File "openjump-1.3forwin\bin\openjump.exe"
  File "openjump-1.3forwin\bin\workbench-properties.xml"
  File "openjump-1.3forwin\bin\lcp.bat"
  File "openjump-1.3forwin\bin\log4j.xml"
  File "openjump-1.3forwin\bin\openjump.sh"
  File "openjump-1.3forwin\bin\openjump.bat"
  File "openjump-1.3forwin\bin\default-plugins.xml"
  CreateDirectory "$SMPROGRAMS\OpenJUMP"
  CreateShortCut "$SMPROGRAMS\OpenJUMP\OpenJUMP.lnk" "$INSTDIR\bin\openjump.exe"
  CreateShortCut "$DESKTOP\OpenJUMP.lnk" "$INSTDIR\bin\openjump.exe"
  SetOutPath "$INSTDIR"
  File "openjump-1.3forwin\readme.txt"
  File "openjump-1.3forwin\jython_license.txt"
  File "openjump-1.3forwin\OJ_improvements_from_v1.2_to_v1.3.txt"
  File "openjump-1.3forwin\Using_MrSIDPlugIn.txt"
  File "openjump-1.3forwin\apache_license.txt"
  File "openjump-1.3forwin\gpl2_license.txt"
  File "openjump-1.3forwin\jmathlicense.txt"
  SetOutPath "$INSTDIR\lib\"
  File "openjump-1.3forwin\lib\batik-all.jar"
  File "openjump-1.3forwin\lib\bsh-2.0b4.jar"
  File "openjump-1.3forwin\lib\Buoy.jar"
  File "openjump-1.3forwin\lib\ermapper.jar"
  File "openjump-1.3forwin\lib\jai_codec.jar"
  File "openjump-1.3forwin\lib\jai_core.jar"
  File "openjump-1.3forwin\lib\Jama-1.0.1.jar"
  File "openjump-1.3forwin\lib\jdom.jar"
  File "openjump-1.3forwin\lib\jmatharray-20070905.jar"
  File "openjump-1.3forwin\lib\jts-1.10.jar"
  File "openjump-1.3forwin\lib\jython.jar"
  File "openjump-1.3forwin\lib\log4j-1.2.8.jar"
  File "openjump-1.3forwin\lib\openjump-api-1.3.jar"
  File "openjump-1.3forwin\lib\openjump-workbench-1.3.jar"
  File "openjump-1.3forwin\lib\postgis_1_0_0.jar"
  File "openjump-1.3forwin\lib\postgresql-8.3-603.jdbc3.jar"
  File "openjump-1.3forwin\lib\xercesImpl.jar"
  File "openjump-1.3forwin\lib\xml-apis.jar"
  File "openjump-1.3forwin\lib\xml-apis-ext.jar"
  SetOutPath "$INSTDIR\lib\ext\"
  File "openjump-1.3forwin\lib\ext\NCScnet.dll"
  File "openjump-1.3forwin\lib\ext\NCSEcw.dll"
  File "openjump-1.3forwin\lib\ext\NCSUtil.dll"
  File "openjump-1.3forwin\lib\ext\readme.txt"
  SetOutPath "$INSTDIR\lib\ext\etc\"
  File "openjump-1.3forwin\lib\ext\etc\mrsidgeodecode.exe"
  File "openjump-1.3forwin\lib\ext\etc\mrsidgeoinfo.exe"
  SetOutPath "$INSTDIR\lib\ext\etc\tmp\"
  SetOutPath "$INSTDIR\lib\ext\BeanTools\"
  File "openjump-1.3forwin\lib\ext\BeanTools\0-Help.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\1-HelloWorld.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\2-NewLayer.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\3-Populate.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\4-AddAttribute.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\5-ChangeAttributeValue.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\6-PushPinPoints.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\7-AddXYAsAttributes.bsh"
  File "openjump-1.3forwin\lib\ext\BeanTools\RefreshScriptsMenu.bsh"
  SetOutPath "$INSTDIR\lib\ext\jython\"
  File "openjump-1.3forwin\lib\ext\jython\AlignSelected.py"
  File "openjump-1.3forwin\lib\ext\jython\ArcTool.py"
  File "openjump-1.3forwin\lib\ext\jython\CircleTool.py"
  File "openjump-1.3forwin\lib\ext\jython\DistributeSelected.py"
  File "openjump-1.3forwin\lib\ext\jython\OvalTool.py"
  File "openjump-1.3forwin\lib\ext\jython\RightAngleTool.py"
  File "openjump-1.3forwin\lib\ext\jython\RoadTool.py"
  File "openjump-1.3forwin\lib\ext\jython\RotatedRectangleTool.py"
  File "openjump-1.3forwin\lib\ext\jython\SetASHSLabel.py"
  File "openjump-1.3forwin\lib\ext\jython\startup.py"
  File "openjump-1.3forwin\lib\ext\jython\UnionSelected.py"
  SetOutPath "$INSTDIR\lib\ext\jython\images\"
  File "openjump-1.3forwin\lib\ext\jython\images\DrawArc.gif"
  File "openjump-1.3forwin\lib\ext\jython\images\DrawCircle.gif"
  File "openjump-1.3forwin\lib\ext\jython\images\DrawCorner.gif"
  File "openjump-1.3forwin\lib\ext\jython\images\DrawOval.gif"
  File "openjump-1.3forwin\lib\ext\jython\images\DrawPoint.gif"
  File "openjump-1.3forwin\lib\ext\jython\images\DrawRoad.gif"
  File "openjump-1.3forwin\lib\ext\jython\images\DrawRotRect.gif"
SectionEnd

Section -AdditionalIcons
  SetOutPath $INSTDIR
  WriteIniStr "$INSTDIR\${PRODUCT_NAME}.url" "InternetShortcut" "URL" "${PRODUCT_WEB_SITE}"
  CreateShortCut "$SMPROGRAMS\OpenJUMP\Website.lnk" "$INSTDIR\${PRODUCT_NAME}.url"
  CreateShortCut "$SMPROGRAMS\OpenJUMP\Uninstall.lnk" "$INSTDIR\uninst.exe"
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\bin\openjump.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\openjump.ico"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
SectionEnd

Function un.onUninstSuccess
  HideWindow
  MessageBox MB_ICONINFORMATION|MB_OK "OpenJUMP has been deinstalled successfully."
FunctionEnd

Function un.onInit
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Would you like to uninstall OpenJUMP and all its components?" IDYES +2
  Abort
FunctionEnd

Function GetJRE
        MessageBox MB_OKCANCEL|MB_ICONEXCLAMATION "${PRODUCT_NAME} uses Java ${JRE_VERSION}, it will now \
                         be downloaded and installed"  IDCANCEL NoDownloadJava
        
        StrCpy $2 "$TEMP\Java Runtime Environment.exe"
        nsisdl::download /TIMEOUT=30000 ${JRE_URL} $2
        Pop $R0 ;Get the return value
                StrCmp $R0 "success" +3
                MessageBox MB_OK "Download failed: $R0"
                ;Quit ;we dont want to quit
        ExecWait $2
        Delete $2
        NoDownloadJava:
                ;Quit ;we dont want to quit
FunctionEnd


Function DetectJRE
  ReadRegStr $2 HKLM "SOFTWARE\JavaSoft\Java Runtime Environment" \
             "CurrentVersion"
  StrCmp $2 ${JRE_VERSION} done

  Call GetJRE

  done:
FunctionEnd

Section Uninstall
  Delete "$INSTDIR\${PRODUCT_NAME}.url"
  Delete "$INSTDIR\uninst.exe"
  Delete "$INSTDIR\lib\ext\BeanTools\*.*"
  Delete "$INSTDIR\lib\ext\jython\images\*.*"
  Delete "$INSTDIR\lib\ext\jython\*.*"
  Delete "$INSTDIR\lib\ext\etc\tmp\*.*"
  Delete "$INSTDIR\lib\ext\etc\*.*"
  Delete "$INSTDIR\lib\ext\*.*"
  Delete "$INSTDIR\lib\*.*"
  Delete "$INSTDIR\bin\*.*"
  Delete "$INSTDIR\*.*"

  Delete "$SMPROGRAMS\OpenJUMP\Uninstall.lnk"
  Delete "$SMPROGRAMS\OpenJUMP\Website.lnk"
  Delete "$DESKTOP\OpenJUMP.lnk"
  Delete "$SMPROGRAMS\OpenJUMP\OpenJUMP.lnk"

  RMDir "$SMPROGRAMS\OpenJUMP"
  RMDir "$INSTDIR\lib\ext\etc\tmp\"
  RMDir "$INSTDIR\lib\ext\etc\"
  RMDir "$INSTDIR\lib\ext\jython\images\"
  RMDir "$INSTDIR\lib\ext\jython\"
  RMDir "$INSTDIR\lib\ext\BeanTools\"
  RMDir "$INSTDIR\lib\ext\"
  RMDir "$INSTDIR\lib\"
  RMDir "$INSTDIR\bin\"
  RMDir "$INSTDIR"

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  SetAutoClose true
SectionEnd