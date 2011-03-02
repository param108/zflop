package 
{


	public class Profiler
	{
		private static var _host:String = "localhost";
		private static var _port:int = 42624;
		private static var _socket:Socket;
		private static var _connected:Boolean = False;
		private static var _failed:Boolean = False;
		
		static public function initSocket():void {
			if(_failed) {
				return;
			}
                        _socket = new Socket();
                        _socket.timeout = 240 * 1000;
                        _socket.addEventListener(SecurityErrorEvent.SECURITY_ERROR, fail);
                        _socket.addEventListener(IOErrorEvent.IO_ERROR, fail);
                        _socket.addEventListener(Event.CLOSE, close);
                        try {
                                _socket.connect(_host, _port);
                        } catch (e:Error) {
                                trace(PREFIX, "Unable to connect", e);
				_failed = true;
				return;
                        }
			_connected = true;
		}

		static public function enterFunction(pkg: String, klass: String, func: String):void
		{
			if (!_connected) {
			    initSocket();
			}
			
			if (_connected){
			trace("["+(new Date().valueOf())+"] Enter "+ func);
			}
		}
		static public function exitFunction(pkg: String, klass: String, func: String):void
		{
			if (!_connected) {
			    initSocket();
			}
			
			if (_connected){
			trace("["+(new Date().valueOf())+"] Exit "+ func);
			}
		}
	}
}
