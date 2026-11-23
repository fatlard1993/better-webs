package justfatlard.better_webs;

import justfatlard.pandorical.api.PandoricalApi;
import net.fabricmc.api.ModInitializer;

public class Main implements ModInitializer {
	public static final String MOD_ID = "better-webs-justfatlard";

	@Override
	public void onInitialize() {
		if (PandoricalApi.isAvailable()) {
			// The strand models are vanilla's cobweb's, so they live in the minecraft namespace.
			PandoricalApi.content().registerModAssets(MOD_ID);
		}

		ThrownWeb.register();
		SpiderSizes.register();

		System.out.println("[" + MOD_ID + "] Loaded");
	}
}
