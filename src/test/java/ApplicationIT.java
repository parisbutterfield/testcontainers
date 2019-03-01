
import com.fasterxml.jackson.databind.ObjectMapper;
import com.parisbutterfield.app.Application;
import com.parisbutterfield.app.dto.User;
import okhttp3.*;
import org.junit.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

public class ApplicationIT {

    static GenericContainer mysql;
    static GenericContainer app;
    static final OkHttpClient client = new OkHttpClient();
    static final Network network = Network.newNetwork();
    static final Logger logger = LoggerFactory.getLogger(Application.class);
    static final ObjectMapper mapper = new ObjectMapper();
    static final int addedUsers = 10;
    static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    static final String userSqlLocation = "/usr/local/data/users.sql";
    static String baseURL;

    @BeforeClass
    public static void setUp() throws Exception {
      mysql = new GenericContainer("mysql:5.7")
                        .withExposedPorts(3306)
                        .withNetwork(network)
                        .withNetworkAliases("mysql")
                        .withEnv("MYSQL_ROOT_PASSWORD", "root")
                        .withClasspathResourceMapping("/sql/bootstrap.sql", "/docker-entrypoint-initdb.d/bootstrap.sql", BindMode.READ_ONLY)
                        .withClasspathResourceMapping("/sql/users.sql" , userSqlLocation, BindMode.READ_WRITE);
      mysql.start();
      app = new GenericContainer("parisbutterfield/testcontainers:latest")
              .withExposedPorts(8080)
              .withNetworkAliases("app")
              .withEnv("spring_datasource_url", "jdbc:mysql://mysql:3306/userdata")
              .withNetwork(network);
      app.start();
      enableContainerLogger(app);
      baseURL = "http://" + app.getContainerIpAddress() + ":" + app.getFirstMappedPort();
      logger.info("URL for external app is " + baseURL);
    }

    public static void enableContainerLogger(GenericContainer container) {
        Slf4jLogConsumer logConsumer =  new Slf4jLogConsumer(logger);
        container.followOutput(logConsumer);
        ToStringConsumer toStringConsumer =  new ToStringConsumer();
        container.followOutput(toStringConsumer);
    }

    /**
     * Truncate the table after every test. This allows data to be clean across @Test
     * @throws Exception
     */
    @After
    public void tearDown() throws Exception {
        mysql.execInContainer("/bin/bash",  "-c" , "mysql -uroot -proot userdata -e \"truncate table user;\"");
    }

    /**
     * Tests the spring boot actuator endpoint. Health is enabled and should return a 200.
     * @throws IOException
     */
    @Test
    public void healthCheck() throws IOException {
        Request request = new Request.Builder()
                .url(baseURL + "/actuator/health")
                .build();
        Response response = client.newCall(request).execute();
        assertTrue(response.code() == 200);
    }


    /**
     * Tests the spring boot actuator endpoint. Info is not enabled and should not be exposed.
     * @throws IOException
     */
    @Test
    public void info() throws IOException {
        Request request = new Request.Builder()
                .url(baseURL + "/actuator/info")
                .build();
        Response response = client.newCall(request).execute();
        assertTrue(response.code() == 404);
    }

    /**
     * Test to add a single user
     * @throws IOException
     */
    @Test
    public void addUser() throws IOException {
        User user = new User();
        user.setEmail("johndoe@xyz.com");
        user.setName("John Doe");
        String payload = mapper.writeValueAsString(user);
        RequestBody body = RequestBody.create(JSON, payload);
        Request request = new Request.Builder()
                .url(baseURL + "/addUser")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertTrue(response.code() == 200);
    }

    /**
     * Test to add a multiple users
     * @throws IOException
     */
    @Test
    public void addUsers() throws IOException {
        List<User> users= new ArrayList<>();

        for(int i = 0; i < addedUsers; i++) {
            String userUUID = UUID.randomUUID().toString();
            User user = new User();
            user.setEmail(userUUID+ "@xyz.com");
            user.setName(userUUID);
            users.add(user);
        }
        String payload = mapper.writeValueAsString(users);
        RequestBody body = RequestBody.create(JSON, payload);
        Request request = new Request.Builder()
                .url(baseURL + "/addUsers")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertTrue(response.body().string().equals(String.valueOf(addedUsers)));
    }

    /**
     * Test to a user with an invalid payload.
     * @throws IOException
     */
    @Test
    public void addUserEmpty() throws IOException {
        RequestBody body = RequestBody.create(JSON, "");
        Request request = new Request.Builder()
                .url(baseURL + "/addUser")
                .post(body)
                .build();
        Response response = client.newCall(request).execute();
        assertTrue(response.code() == 400);
    }

    /**
     * Test to count the number of users. A script to add users is run on the mysql instance.
     * The we assert the number users matches using the /count endpoint.
     * @throws IOException
     * @throws InterruptedException
     */
    @Test
    public void countUsers() throws IOException, InterruptedException {
        mysql.execInContainer("/bin/bash",  "-c" , "mysql -uroot -proot userdata < " +  userSqlLocation);
        Request request = new Request.Builder()
                .url(baseURL + "/count")
                .build();
        Response response = client.newCall(request).execute();
        assertTrue(response.body().string().equals(String.valueOf(5)));
    }

    /**
     * This tests adds a user and then using the inserted user id, retrieves it.
     * After retrieving the object, we compare with the original object.
     * @throws IOException
     */
    @Test
    public void compare() throws IOException {
        User user = new User();
        user.setEmail("johndoe@xyz.com");
        user.setName("John Doe");
        String payload = mapper.writeValueAsString(user);
        RequestBody postBody = RequestBody.create(JSON, payload);
        Request postRequest = new Request.Builder()
                .url(baseURL + "/addUser")
                .post(postBody)
                .build();
        Response postResponse = client.newCall(postRequest).execute();
        String id = postResponse.body().string();

        Request getRequest = new Request.Builder()
                .url(baseURL + "/user/" + id)
                .build();
        Response getResponse = client.newCall(getRequest).execute();
        User requestUser = mapper.readValue(getResponse.body().string(), User.class);
        assertTrue(requestUser.equals(user));
    }
}