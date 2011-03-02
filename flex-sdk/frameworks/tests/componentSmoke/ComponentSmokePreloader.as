package 
{
import flash.system.ApplicationDomain;
import mx.preloaders.DownloadProgressBar;

public class ComponentSmokePreloader extends DownloadProgressBar
{
	public function ComponentSmokePreloader()
	{
		super();
		if (ApplicationDomain.currentDomain.hasDefinition("mx.core::UIComponent"))
		{
			throw new Error("UIComponent found in frame 1");
		}
	}
}

}

