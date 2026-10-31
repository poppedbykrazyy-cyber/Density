import numpy as np, wave
SR=48000; D=30.0; N=int(SR*D); t=np.arange(N)/SR
rng=np.random.default_rng(3)
def env(a,b,fi=.5,fo=.5):
    return np.clip((t-a)/fi,0,1)*np.clip((b-t)/fo,0,1)
L=np.zeros(N); R=np.zeros(N)
# drone
pad=np.zeros(N)
for f,amp,det in [(55,.30,.0),(82.41,.18,.3),(110,.14,.6),(164.8,.06,.9),(220,.04,1.3)]:
    for d in (-det,det):
        pad+=amp*np.sin(2*np.pi*(f+d*.4)*t+rng.uniform(0,6))
pad*=0.55+0.45*np.sin(2*np.pi*t/7.5)**2
padenv=np.clip(t/2.5,0,1)*np.clip((30-t)/1.2,0,1)*(1-0.7*np.exp(-((t-26.6)/0.5)**2))
pad*=padenv*.22
# shimmer
sh=(np.sin(2*np.pi*880*t)*.5+np.sin(2*np.pi*1318.5*t)*.35+np.sin(2*np.pi*1760*t)*.2)*(0.5+0.5*np.sin(2*np.pi*3.1*t))
sh*=(env(0.5,5.2,1.5,.8)+env(27.2,30,1,.8))*.02
def impact(tc,amp=1.0):
    x=np.zeros(N); i=int(tc*SR); n=int(2.5*SR); tt=np.arange(n)/SR
    f=35+60*np.exp(-tt*9); ph=2*np.pi*np.cumsum(f)/SR
    s=np.sin(ph)*np.exp(-tt*2.2)
    nz=rng.standard_normal(n)*np.exp(-tt*14)*.35
    k=np.ones(40)/40; nz=np.convolve(nz,k,'same')*4
    seg=(s+nz)*amp; e=min(N,i+n); x[i:e]+=seg[:e-i]; return x
def whoosh(tc,dur=1.1,amp=.5,rise=True):
    x=np.zeros(N); n=int(dur*SR); i=int((tc-dur*.7)*SR); tt=np.arange(n)/SR
    nz=rng.standard_normal(n)
    w=int(30+60*(1)) 
    nz=np.convolve(nz,np.ones(24)/24,'same')*3 - np.convolve(nz,np.ones(400)/400,'same')*3
    e=np.sin(np.pi*np.clip(tt/dur,0,1))**2
    seg=nz*e*amp; a,b=max(0,i),min(N,i+n); x[a:b]+=seg[a-i:b-i]; return x
def click(tc,amp=.35,f=2400,dec=60):
    x=np.zeros(N); n=int(.08*SR); i=int(tc*SR); tt=np.arange(n)/SR
    s=(np.sin(2*np.pi*f*tt)*.6+rng.standard_normal(n)*.4)*np.exp(-tt*dec)
    x[i:i+n]+=s*amp; return x
def blip(tc,amp=.12,f=660):
    x=np.zeros(N); n=int(.35*SR); i=int(tc*SR); tt=np.arange(n)/SR
    s=(np.sin(2*np.pi*f*tt)+.3*np.sin(2*np.pi*2*f*tt))*np.exp(-tt*9)*np.clip(tt/.004,0,1)
    x[i:i+n]+=s*amp; return x
fx=np.zeros(N)
fx+=impact(3.0,.9)+impact(27.6,1.0)
fx+=whoosh(4.9,1.2,.35)+whoosh(8.3,1.4,.4)+whoosh(11.0,.9,.18)+whoosh(26.8,1.6,.45)
for c in (12.55,19.6,20.7): fx+=click(c,.32)
fx+=click(15.85,.18,1800)
for k in range(6): fx+=click(15.9+k*(1.1/6)+.02,.14,3200,90)
for k in range(9): fx+=blip(21.4+k*.5,.09,[523.25,587.3,659.3,784,880,784,659.3,587.3,523.25][k])
fx+=blip(9.05,.07,440)+blip(10.55,.08,880)
dry=pad+sh+fx
# reverb: convolve with decaying noise IR (stereo decorrelated)
def rev(x,seed):
    r=np.random.default_rng(seed); n=int(2.2*SR); tt=np.arange(n)/SR
    ir=r.standard_normal(n)*np.exp(-tt*2.6); ir[:int(.012*SR)]=0
    m=1<<int(np.ceil(np.log2(len(x)+n)))
    y=np.fft.irfft(np.fft.rfft(x,m)*np.fft.rfft(ir,m),m)[:len(x)]
    return y/np.max(np.abs(y))*np.max(np.abs(x))*.6
wl=rev(dry,1); wr=rev(dry,2)
L=dry*.8+wl*.45; R=dry*.8+wr*.45
mx=max(np.abs(L).max(),np.abs(R).max()); L/=mx/0.89; R/=mx/0.89
fade=np.clip((30-t)/0.6,0,1); L*=fade; R*=fade
out=(np.stack([L,R],1)*32767).astype(np.int16)
with wave.open('audio.wav','wb') as w: w.setnchannels(2); w.setsampwidth(2); w.setframerate(SR); w.writeframes(out.tobytes())
print('ok')
