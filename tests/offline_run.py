"""Run a verification command with IPv4/IPv6 sockets denied by Linux seccomp."""
import ctypes,os,sys,socket,platform
assert platform.machine()=='x86_64'
class Filter(ctypes.Structure):_fields_=[('code',ctypes.c_ushort),('jt',ctypes.c_ubyte),('jf',ctypes.c_ubyte),('k',ctypes.c_uint)]
class Program(ctypes.Structure):_fields_=[('len',ctypes.c_ushort),('filter',ctypes.POINTER(Filter))]
# nr == socket(41): deny AF_INET(2) / AF_INET6(10); allow other system calls.
f=(Filter*7)(Filter(0x20,0,0,0),Filter(0x15,0,4,41),Filter(0x20,0,0,16),Filter(0x15,1,0,2),Filter(0x15,0,1,10),Filter(0x06,0,0,0x00050000|101),Filter(0x06,0,0,0x7fff0000))
p=Program(7,f);libc=ctypes.CDLL(None,use_errno=True)
if libc.prctl(38,1,0,0,0)!=0 or libc.prctl(22,2,ctypes.byref(p),0,0)!=0:raise OSError(ctypes.get_errno(),'Cannot enforce network block')
for family in [socket.AF_INET,socket.AF_INET6]:
 try:socket.socket(family);raise RuntimeError('Network guard failed')
 except OSError as e:assert e.errno==101
print('IPv4/IPv6 sockets blocked for this process and all children.',flush=True)
os.environ['ORT_DISABLE_TELEMETRY']='1';os.execvp(sys.argv[1],sys.argv[1:])
