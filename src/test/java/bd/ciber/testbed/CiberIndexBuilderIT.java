package bd.ciber.testbed;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.mongodb.Mongo;
import com.mongodb.MongoClient;

import de.flapdoodle.embed.mongo.MongodExecutable;
import de.flapdoodle.embed.mongo.MongodProcess;
import de.flapdoodle.embed.mongo.MongodStarter;
import de.flapdoodle.embed.mongo.config.MongodConfigBuilder;
import de.flapdoodle.embed.mongo.config.Net;
import de.flapdoodle.embed.mongo.distribution.Version;
import de.flapdoodle.embed.process.runtime.Network;

public class CiberIndexBuilderIT {
	/**
     * please store Starter or RuntimeConfig in a static final field
     * if you want to use artifact store caching (or else disable caching)
     */
    private static final MongodStarter starter = MongodStarter.getDefaultInstance();

    private MongodExecutable _mongodExe;
    private MongodProcess _mongod;

    private MongoClient _mongo;
    
    @Before
    public void setup() throws Exception {
        _mongodExe = starter.prepare(new MongodConfigBuilder()
            .version(Version.Main.PRODUCTION)
            .net(new Net(12345, Network.localhostIsIPv6()))
            .build());
        _mongod = _mongodExe.start();
        _mongo = new MongoClient("localhost", 12345);
    }

    @After
    public void tearDown() throws Exception {
        _mongod.stop();
        _mongodExe.stop();
    }

    public Mongo getMongo() {
        return _mongo;
    }
    
    // TODO make sure that we index that there is no extensions in those cases
    
    @Test
    public void testRebuild() throws IOException {
    	CiberIndexBuilder builder = new CiberIndexBuilder();
    	builder.setMongoClient(_mongo);
    	File f = new File("src/test/resources/inventory.txt");
    	builder.rebuild(f);
    }

}
