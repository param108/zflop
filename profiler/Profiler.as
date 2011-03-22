package 
{
   	import flash.events.Event;
   	import flash.events.IOErrorEvent;
   	import flash.events.SecurityErrorEvent;
   	import flash.net.Socket;
   	import flash.sampler.*;
   	import flash.system.Security;
   	import flash.system.System;
   	import flash.utils.ByteArray;
   	import flash.utils.getTimer;
   	
	public class Profiler
	{
		private static const PREFIX:String = "Profiler:";
		private static var _host:String = "localhost";
		private static var _port:int = 42426;
		private static var _socket:Socket;
		private static var _connected:Boolean = false;
		private static var _failed:Boolean = false;
		
		private static var _stack:Array = [];
		private static var _xhprof:Object = {};
		private static var _dumptime:int = 0;

		static public function initSocket():void {
			if(_failed) {
				return;
			}

			//Security.loadPolicyFile('http://127.0.0.1/crossdomain.xml');
			Security.allowDomain("*");
			
			trace(PREFIX, "Opening port ", _port);
			_socket = new Socket();

            //_socket.timeout = 240 * 1000;
            _socket.addEventListener(SecurityErrorEvent.SECURITY_ERROR, fail);
            _socket.addEventListener(IOErrorEvent.IO_ERROR, fail);
            _socket.addEventListener(Event.CLOSE, close);

			try {
				_socket.connect(_host, _port);
				_connected = true;
			} catch (e:Error) {
				trace(PREFIX, "Unable to connect", e);
				_failed = true;
			}
		}

		static public function enterFunction(pkg: String, klass: String, func: String):void
		{
			var start:int = getTimer();
			var mem:int = System.totalMemory;
			_stack.push([klass+"::"+func, start, mem]);
		}

		static public function exitFunction(pkg: String, klass: String, func: String):void
		{
			var shouldbe:Object = null;
			var parent:Object = null;
			var key:String = klass+"::"+func;
			var xhprof_key:String = null;
			var end:int = getTimer();
			var mem:int = System.totalMemory;
			
			do {
				if(_stack.length > 0) {
					shouldbe = _stack.pop();
					parent = _stack[_stack.length - 1]; // peek
					if(parent == null) parent=[["main()"]];
					xhprof_key = parent[0]+"==>"+shouldbe[0];
					if(_xhprof[xhprof_key] == null) {
						_xhprof[xhprof_key] = {"wt":0.0, "ct": 0, "mu":0};
					}
					_xhprof[xhprof_key]["wt"] += 1000*(end-shouldbe[1]); // micro-seconds 
					_xhprof[xhprof_key]["ct"] += 1;
					_xhprof[xhprof_key]["mu"] += (mem-shouldbe[2]);
				} else {
					break;
				}
			} while(shouldbe != null && shouldbe[0] != key)
			
			if(_stack.length == 0 && _dumptime < end) {
				_dumptime = end + 3*1000;
				if(!_connected) {
					initSocket();
				}
				if(_connected && !_failed) {
					// write data to sockets
					for(key in _xhprof) {
						var v:Object = _xhprof[key];
						_socket.writeUTFBytes(key);
						_socket.writeByte(2);
						_socket.writeUTFBytes(v["wt"]); // micro seconds conversion
						_socket.writeByte(2);
						_socket.writeUTFBytes(v["ct"]);
						_socket.writeByte(2);
						_socket.writeUTFBytes(v["mu"]);
						_socket.writeUTFBytes("\n"); // \n
					}
trace("Flushing socket");
					_socket.flush();
					_xhprof = {};
				}
			}
		}

		
		private static function close(e:Event):void
        {
            _connected = false;
			_failed = true;
            trace(PREFIX, "Disconnected");
        }

                private static function fail(e:Event):void
                {
                        trace(PREFIX, "Communication failure", e);

                        _socket.close();
			_failed = true;
                        _connected = false;
                }

	}
}
