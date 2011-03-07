package 
{
    public class Greeter 
    {
        public function sayHello():String 
        {
			try {
			exceptionalCase();
			}
			catch(e:Error)
			{
				return "Hello World!";
			}
			return "This Can't Be Right";
        }

		public function exceptionalCase():void
		{
				throw new Error("Just because");
		}
    }

}


