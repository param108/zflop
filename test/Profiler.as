package 
{
			public class Profiler
			{
				static public function enterFunction(pkg: String, klass: String, func: String):void
				{
					trace("["+(new Date().valueOf())+"] Enter "+ func);
				}
				static public function exitFunction(pkg: String, klass: String, func: String):void
				{
					trace("["+(new Date().valueOf())+"] Exit "+ func);
				}
			}
}
