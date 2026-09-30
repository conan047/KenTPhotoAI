from pathlib import Path
import xml.etree.ElementTree as ET

root = Path(__file__).resolve().parent
required = [
    'settings.gradle','build.gradle','app/build.gradle','app/src/main/AndroidManifest.xml',
    'app/src/main/java/com/ken93/kenphotoai/MainActivity.java',
    'app/src/main/java/com/ken93/kenphotoai/ImageProcessor.java',
    'app/src/main/java/com/ken93/kenphotoai/CommandParser.java',
    'app/src/main/java/com/ken93/kenphotoai/EnhanceProfile.java',
    'app/src/main/java/com/ken93/kenphotoai/BitmapIo.java',
    'app/src/main/java/com/ken93/kenphotoai/UpscaleEngine.java',
    '.github/workflows/build-apk.yml',
    'BUILD_APK_WINDOWS.bat',
]
missing = [p for p in required if not (root / p).exists()]
if missing:
    raise SystemExit('MISSING: ' + ', '.join(missing))

java_text = '\n'.join((root / p).read_text(encoding='utf-8') for p in required if p.endswith('.java'))
if '.isBlank()' in java_text:
    raise SystemExit('Compatibility warning: String.isBlank() found')

app_gradle = (root / 'app/build.gradle').read_text(encoding='utf-8')
assert "compileSdk 36" in app_gradle
assert "targetSdk 36" in app_gradle
assert "versionCode 3" in app_gradle
assert "versionName '1.2.0'" in app_gradle

root_gradle = (root / 'build.gradle').read_text(encoding='utf-8')
assert "version '9.3.0'" in root_gradle

ET.parse(root / 'app/src/main/AndroidManifest.xml')
for xml in (root / 'app/src/main/res').rglob('*.xml'):
    ET.parse(xml)

workflow = (root / '.github/workflows/build-apk.yml').read_text(encoding='utf-8')
for expected in ['actions/checkout@v7', 'actions/setup-java@v6', 'android-actions/setup-android@v4', 'gradle/actions/setup-gradle@v6', 'actions/upload-artifact@v7']:
    assert expected in workflow, expected

print('KenPhoto AI v1.2 project structure/XML/static checks: OK')
