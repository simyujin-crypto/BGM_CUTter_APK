// `npx cap add android` 로 만들어진 프로젝트에 벨소리 플러그인·권한·아이콘을 끼워 넣는다.
const fs = require('fs'), path = require('path');
const pkgDir = 'com/travelstudio/bgmcutter', base = 'android/app/src/main';
const must = (p) => { if (!fs.existsSync(p)) { console.error('없음: ' + p); process.exit(1); } };

// 1) 플러그인 + MainActivity
fs.mkdirSync(`${base}/java/${pkgDir}`, { recursive: true });
fs.copyFileSync('native/RingtonePlugin.java', `${base}/java/${pkgDir}/RingtonePlugin.java`);
fs.writeFileSync(`${base}/java/${pkgDir}/MainActivity.java`,
`package com.travelstudio.bgmcutter;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(RingtonePlugin.class);
        super.onCreate(savedInstanceState);
    }
}
`);

// 2) 권한: 시스템 설정 변경(기본 벨소리 지정용)
const mf = `${base}/AndroidManifest.xml`; must(mf);
let m = fs.readFileSync(mf, 'utf8');
if (!m.includes('WRITE_SETTINGS')) {
  m = m.replace('</manifest>', '    <uses-permission android:name="android.permission.WRITE_SETTINGS" />\n</manifest>');
  fs.writeFileSync(mf, m);
}

// 3) 최소 안드로이드 10 (MediaStore 로 별도 저장 권한 없이 저장)
const vg = 'android/variables.gradle'; must(vg);
fs.writeFileSync(vg, fs.readFileSync(vg, 'utf8').replace(/minSdkVersion\s*=\s*\d+/, 'minSdkVersion = 29'));

// 4) 아이콘 교체
const res = `${base}/res`;
for (const d of fs.readdirSync(res)) {
  const dir = path.join(res, d);
  if (d === 'mipmap-anydpi-v26') { fs.rmSync(dir, { recursive: true, force: true }); continue; }
  if (!d.startsWith('mipmap-')) continue;
  for (const f of ['ic_launcher.png', 'ic_launcher_round.png', 'ic_launcher_foreground.png'])
    fs.copyFileSync('www/icon-192.png', path.join(dir, f));
}
console.log('patch 완료');
