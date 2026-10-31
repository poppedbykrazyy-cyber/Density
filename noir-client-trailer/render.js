const { chromium } = require('playwright');const { spawn } = require('child_process');
(async()=>{
  const FPS=30,DUR=30,N=FPS*DUR;
  const ff=spawn('ffmpeg',['-y','-loglevel','error','-f','image2pipe','-framerate',String(FPS),'-c:v','mjpeg','-i','-','-c:v','libx264','-preset','slow','-crf','16','-pix_fmt','yuv420p','video.mp4']);
  ff.stderr.pipe(process.stderr);
  const b=await chromium.launch();const p=await b.newPage({viewport:{width:1920,height:1080}});
  await p.goto('file://'+__dirname+'/index.html');await p.evaluate(()=>document.fonts.ready);
  for(let f=0;f<N;f++){await p.evaluate(t=>render(t),f/FPS);const buf=await p.screenshot({type:'jpeg',quality:95});
    if(!ff.stdin.write(buf))await new Promise(r=>ff.stdin.once('drain',r));if(f%150===0)console.log('frame',f)}
  ff.stdin.end();await new Promise(r=>ff.on('close',r));await b.close();console.log('done');
})();
