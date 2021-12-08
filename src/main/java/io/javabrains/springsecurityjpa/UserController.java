package io.javabrains.springsecurityjpa;

import org.hibernate.Session;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.document.spec.GetItemSpec;
import com.amazonaws.services.dynamodbv2.model.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClientBuilder;
import com.amazonaws.services.sns.model.PublishRequest;
import io.javabrains.springsecurityjpa.models.User;
import io.javabrains.springsecurityjpa.models.UserPic;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.timgroup.statsd.StatsDClient;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    private final UserRepository userRepository;

    @Autowired
    private final ImageRepository imageRepository;

//    @Autowired
//    private final UserReadReplicaOnlyRepository userReadReplicaOnlyRepository;
//
//    @Autowired
//    private final ImageReadReplicaOnlyRepository imageReadReplicaOnlyRepository;

    @Autowired
    private AmazonS3 amazonS3;

    @Autowired
    private StatsDClient statsd;

    AmazonDynamoDB dynamodbClient;

    AmazonSNS snsClient;

    Long expirationTTL;

    @Value("${snstopic}")
    private String snstopic;

    //amazonS3 = AmazonS3ClientBuilder.standard().withRegion(Regions.DEFAULT_REGION).build();

    @Value("${bucketName}")
    private String bucket;

    public UserController(UserRepository userRepository, ImageRepository imageRepository) {
        this.userRepository = userRepository;
        this.imageRepository = imageRepository;
//        this.imageReadReplicaOnlyRepository = imageReadReplicaOnlyRepository;
//        this.userReadReplicaOnlyRepository = userReadReplicaOnlyRepository;
    }
    //private String bucketURL="https://s3.console.aws.amazon.com/s3/buckets/csye6225.prod.domain.tld?region=us-east-1&tab=objects";

//    private String bucket = "csye6225.prod.domain.tld";
//    private String bucketURL = "https://s3.console.aws.amazon.com/s3/buckets/csye6225.prod.domain.tld?region=us-east-1&tab=objects";

//    @GetMapping("/")
//    public String home() {
//        return ("<h1>Welcome</h1>");
//    }

//    @GetMapping("/v1/user/self")
//    public ResponseEntity<User> user(Authentication authentication) {
//        statsd.incrementCounter("GetUserDetailsApi");
//        long start = System.currentTimeMillis();
//        User user = userRepository.findByUserName(authentication.getName())
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
//        long end = System.currentTimeMillis();
//        long dbTimeElapsed = end - start;
//        long timeElapsed = end - start;
//        statsd.recordExecutionTime("GetUserFromDBTime", dbTimeElapsed);
//        statsd.recordExecutionTime("GetUserDetailsApiTime", timeElapsed);
//        logger.info("**********User details fetched successfully !**********");
//        if(user.isVerified()){
//            return ResponseEntity.ok(user);
//        }else{
//            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
//        }
//
//    }

    @GetMapping("/v1/user/self")
    public ResponseEntity<User> user(Authentication authentication) {
        statsd.incrementCounter("GetUserDetailsApi");
        long start = System.currentTimeMillis();
//        boolean userExist = false;
//        User user = new User();
        logger.info("**********User details method !**********");
        try {
            Session session = DAO.getSessionFactoryReplica().openSession();
            logger.info("**********Session initialized !**********");
            List<User> result = session.createQuery("from User").list();
            logger.info("**********User create query result !**********");
            logger.info("**********session transaction commit !**********");
            long end = System.currentTimeMillis();
            long dbTimeElapsed = end - start;
            long timeElapsed = end - start;
            statsd.recordExecutionTime("GetUserFromDBTime", dbTimeElapsed);
            statsd.recordExecutionTime("GetUserDetailsApiTime", timeElapsed);
            logger.info("**********User details fetched successfully !**********");
            session.close();
            logger.info("**********session closed !**********");
            for (User user : result) {
                if (user.getUserName().equalsIgnoreCase(authentication.getName())) {
                    if (user.isVerified()) {
                        return ResponseEntity.ok(user);
                    } else {
                        return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
                    }
                }
            }
            return new ResponseEntity<>(null, HttpStatus.NOT_FOUND);
        } catch (Exception e){
            logger.info(e.getMessage());
            logger.info(e.getStackTrace().toString());
            return new ResponseEntity<>(null, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    // build update user REST API
    @PutMapping("/v1/user/self")
    public ResponseEntity<User> updateUser(Authentication authentication, @RequestBody User userDetails) {
        try {
            User user = userRepository.findByUserName(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
            userRepository.flush();
            if (user.isVerified()) {
                statsd.incrementCounter("UpdateUserDetailsAPI");
                long start = System.currentTimeMillis();
                if (userDetails.getUserName() != null && !userDetails.getUserName().isEmpty()) {
                    logger.info("**********Cannot Update email ! **********");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }

                if (userDetails.getAccount_created() != null) {
                    logger.info("**********Cannot Update Account Created details ! **********");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }

                if (userDetails.getAccount_updated() != null) {
                    logger.info("**********Cannot Update Account Updated details ! **********");
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }

                if ((userDetails.getFirst_name() != null && !userDetails.getFirst_name().isEmpty())
                        && (userDetails.getLast_name() != null && !userDetails.getLast_name().isEmpty()) &&
                        (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty())) {
                    String password = userDetails.getPassword();
                    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                    userDetails.setPassword(bCryptPasswordEncoder.encode(password));
                    userDetails.setAccount_updated(new Timestamp(System.currentTimeMillis()));
                    //System.out.println(">>>>>>>>>>>>>>>>>Pass -" + userDetails.getPassword());
                    long dbStart = System.currentTimeMillis();
                    userRepository.updateUser(authentication.getName(), userDetails.getFirst_name(),
                            userDetails.getLast_name(), userDetails.getPassword(), userDetails.getAccount_updated());
                    long end = System.currentTimeMillis();
                    long dbTimeElapsed = end - dbStart;
                    long timeElapsed = end - start;
                    statsd.recordExecutionTime("saveUserToDBTime", dbTimeElapsed);
                    statsd.recordExecutionTime("createNewUserApiTime", timeElapsed);
                    logger.info("**********Creating New User**********");
                    return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
                }

                if (userDetails.getFirst_name() != null && !userDetails.getFirst_name().isEmpty()) {
                    userDetails.setAccount_updated(new Timestamp(System.currentTimeMillis()));
                    userRepository.updateUserFirstName(authentication.getName(), userDetails.getFirst_name(), userDetails.getAccount_updated());
                }
                if (userDetails.getLast_name() != null && !userDetails.getLast_name().isEmpty()) {
                    userDetails.setAccount_updated(new Timestamp(System.currentTimeMillis()));
                    userRepository.updateUserLastName(authentication.getName(), userDetails.getLast_name(), userDetails.getAccount_updated());
                }

                if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                    String password = userDetails.getPassword();
                    BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                    userDetails.setPassword(bCryptPasswordEncoder.encode(password));
                    //System.out.println(">>>>>>>>>>>>>>>>>Pass -" + userDetails.getPassword());
                    userRepository.updateUserPassword(authentication.getName(), userDetails.getPassword(), userDetails.getAccount_updated());
                }
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            } else {
                return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
            }
        }catch (Exception exception) {
            exception.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

//    private ResponseEntity<User> getUserDetails(Authentication authentication) {
//        User user = userRepository.findByUserName(authentication.getName())
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
//        return ResponseEntity.ok(user);
//    }


    @PostMapping("/v1/user")
    public ResponseEntity<User> registerUser(@RequestBody User newUser) {
        try {
            statsd.incrementCounter("CreateUserAPI");
            long start = System.currentTimeMillis();
            System.out.println("PosT=======Entered=============================");
            if (!isValidEmailAddress(newUser.getUserName())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
            System.out.println("PosT=====================================");
            List<User> users = userRepository.findAll();
            //System.out.println("New user: " + newUser.toString());
            for (User user : users) {
                //System.out.println("Registered user: " + newUser.toString());
                if (user.getUserName().equals(newUser.getUserName())) {
                    // System.out.println("User Already exists!");
                    logger.info("**********User account already exists with this email ! **********");
                    long end = System.currentTimeMillis();
                    long timeElapsed = end - start;
                    statsd.recordExecutionTime("createNewUserApiTime", timeElapsed);
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
                }
            }
            dynamodbClient = AmazonDynamoDBClientBuilder.defaultClient();
            logger.info("DynamoDbClient built successfully");
            logger.info("---------------------------------");
            logger.info(dynamodbClient.toString());
            logger.info("---------------------------------");

            Instant currentInstant = Instant.now();
            logger.info("--------------Current Instant-------------------" + currentInstant);
            Instant expirationInstant = currentInstant.plusSeconds(300);
            logger.info("--------------Expiration Instant-------------------" + expirationInstant);
            expirationTTL = expirationInstant.getEpochSecond();
            logger.info("--------------Expiration TTL-------------------" + expirationTTL);
            logger.info("Expiration TTL : "+expirationTTL);
            String token = UUID.randomUUID().toString();

            PutItemRequest request = new PutItemRequest();
            request.setTableName("csye6225-dynamo");
            request.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);
            request.setReturnValues(ReturnValue.ALL_OLD);
            Map<String, AttributeValue> map = new HashMap<>();
            map.put("AccessToken", new AttributeValue(token));
            map.put("id", new AttributeValue(newUser.getUserName()));
            map.put("TTL", new AttributeValue(expirationTTL.toString()));
            request.setItem(map);
            dynamodbClient.putItem(request);
            logger.info("Dynamodb put successful");

            snsClient = AmazonSNSClientBuilder.defaultClient();

            JSONObject json = new JSONObject();
            json.put("AccessToken", token);
            json.put("EmailAddress",newUser.getUserName());
            json.put("MessageType","email");
            PublishRequest publishReq = new PublishRequest()
                    .withTopicArn(snstopic)
                    .withMessage(json.toString());
            snsClient.publish(publishReq);
            BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
            newUser.setPassword(bCryptPasswordEncoder.encode(newUser.getPassword()));
            newUser.setVerified(false);
            newUser.setVerified_on(new Timestamp(System.currentTimeMillis()));
            if (saveDetail(newUser, userRepository, start, statsd)) {
                User user = userRepository.findByUserName(newUser.getUserName())
                        .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + newUser.getUserName()));
                long end = System.currentTimeMillis();
                long timeElapsed = end - start;
                logger.info("Time taken by save user api call is " + timeElapsed + "ms");
                statsd.recordExecutionTime("createUserAPITime",timeElapsed);
                logger.info("**********User details save successfully !**********");
                return ResponseEntity.status(HttpStatus.CREATED).body(user);
            } else {
                logger.info("**********Incorrect Request from User**********");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } catch (Exception e){
            logger.info("**********Exception while creating New User**********");
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    @GetMapping("/v1/verifyUserEmail")
    public ResponseEntity<String> verifyUserEmail(@RequestParam("email") String header_email, @RequestParam("token") String header_token ) {
        User user = userRepository.findByUserName(header_email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + header_email));
        userRepository.flush();
        if (!user.isVerified()) {
            statsd.incrementCounter("Verify User API");
            try {
                logger.info("**********Verify Method**********");
                logger.info("**********header email**********" + header_email);
                logger.info("**********header token**********" + header_token);
                dynamodbClient = AmazonDynamoDBClientBuilder.defaultClient();
                DynamoDB dynamoDB = new DynamoDB(dynamodbClient);

                logger.info("**********header_token**********" + header_token);

                Table table = dynamoDB.getTable("csye6225-dynamo");
                logger.info("**********dynamo Table **********" + table.toString());
                GetItemSpec spec = new GetItemSpec()
                        .withPrimaryKey("id", header_email);
                logger.info("**********getItem spec **********" + spec.toString());
//            Item item = table.getItem(spec);
                Item item = table.getItem("id", header_email);
                logger.info("**********item**********" + item.toJSONPretty());
                logger.info("**********item token value**********" + item.get("AccessToken"));
                logger.info("**********item TTL value**********" + item.get("TTL"));
                boolean tokenCheck = false;
                boolean ttlCheck = false;
                if (item.get("AccessToken").equals(header_token)) {
                    tokenCheck = true;
                    logger.info("**********item Token check**********" + "True");
                } else {
                    return new ResponseEntity<>("Invalid token", HttpStatus.UNAUTHORIZED);
                }
                logger.info("******************TTL***********" + Long.parseLong(item.get("TTL").toString()));
                logger.info("******************Instant now***********" + Long.parseLong(String.valueOf(Instant.now().getEpochSecond())));
                if (Long.parseLong(item.get("TTL").toString()) >= Long.parseLong(String.valueOf(Instant.now().getEpochSecond()))) {
                    ttlCheck = true;
                    logger.info("**********item TTL check**********" + "True");

                } else {
                    return new ResponseEntity<>("Expired token", HttpStatus.UNAUTHORIZED);
                }
                if (tokenCheck && ttlCheck) {
                    logger.info("**********inside if check**********");
                    userRepository.flush();
                    userRepository.updateUserVerified(header_email, true, new Timestamp(System.currentTimeMillis()), new Timestamp(System.currentTimeMillis()));
                    logger.info("**********user details update success**********");
                    return new ResponseEntity<>("User Verified", HttpStatus.OK);
                }
                logger.info("**********outside if check**********");

                return new ResponseEntity<>("User Not Found", HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                logger.info("**********Exception!**********");
                logger.info(e.toString());
                return new ResponseEntity<>("Exception - Bad Request", HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>("User Already Verified", HttpStatus.OK);
        }
    }

    public boolean isValidEmailAddress(String email) {
        String ePattern = "^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@((\\[[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\])|(([a-zA-Z\\-0-9]+\\.)+[a-zA-Z]{2,}))$";
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(ePattern);
        java.util.regex.Matcher m = p.matcher(email);
        return m.matches();
    }


    public static boolean saveDetail(User newUser, UserRepository userRepository, long start, StatsDClient statsd) {
        try {
            newUser.setId(UUID.randomUUID());
            newUser.setAccount_created(new Timestamp(System.currentTimeMillis()));
            newUser.setAccount_updated(new Timestamp(System.currentTimeMillis()));
            newUser.setActive(true);
            // System.out.println(System.currentTimeMillis());
            long dbStart = System.currentTimeMillis();
            userRepository.save(newUser);
            long end = System.currentTimeMillis();
            long dbTimeElapsed = end - dbStart;
            long timeElapsed = end - start;
            statsd.recordExecutionTime("saveUserToDBTime", dbTimeElapsed);
            statsd.recordExecutionTime("createNewUserApiTime", timeElapsed);
            logger.info("**********Creating New User**********");
            //userRepository.save(newUser);
            return true;
        } catch (Exception exception) {
            logger.info("**********Exception while creating New User**********");
            exception.printStackTrace();
            return false;
        }
    }

    @PostMapping("/v1/user/self/pic")
    public ResponseEntity addUpdatePic(Authentication authentication, @RequestBody byte[] binaryFile) {
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
        userRepository.flush();
        if (user.isVerified()) {
            statsd.incrementCounter("AddUserPicAPI");
            long start = System.currentTimeMillis();
            try {
                String fileUrl = "";
                String fileName = generateFileName();
                File file = new File(fileName);
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(binaryFile);
                fos.close();
                Timestamp updateDate = new Timestamp(System.currentTimeMillis());

                UserPic picData = imageRepository.getUserData(user.getId().toString());
                imageRepository.flush();
                //File file = convertMultiPartToFile(multipartFile);
                String fileNameWithDate = new Date().getTime() + "-" + fileName.replace(" ", "_");
//            fileUrl = bucketURL+"/"+bucket+"/"+fileNameWithDate;
                String userID = user.getId().toString();
                fileUrl = userID + "/" + fileNameWithDate;

                if (picData != null) {
                    amazonS3.deleteObject(bucket, picData.getUrl());
//                picData.setUploadDate(new Timestamp(System.currentTimeMillis()));
//                picData.setFileName(fileNameWithDate);
//                picData.setUrl(fileUrl);
                    long dbBookImageUploadToS3Start = System.currentTimeMillis();
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> Update Pic");
                    amazonS3.putObject(bucket, fileUrl, file);
                    long dbBookImageUploadToS3End = System.currentTimeMillis();
                    long dbBookImageUploadToS3TimeElapsed = dbBookImageUploadToS3End - dbBookImageUploadToS3Start;
                    statsd.recordExecutionTime("uploadImageToS3Time", dbBookImageUploadToS3TimeElapsed);
                    imageRepository.updatePic(userID, fileNameWithDate, fileUrl, new Timestamp(System.currentTimeMillis()));
                    long end = System.currentTimeMillis();
                    long timeElapsed = end - start;
                    statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
                    logger.info("**********Image uploaded to S3 bucket successfully**********");
                    picData = imageRepository.getUserData(user.getId().toString());
                    return new ResponseEntity<>(picData, HttpStatus.CREATED);
                } else {
                    System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>> New Pic");
                    picData = new UserPic(fileNameWithDate, fileUrl, updateDate, user.getId().toString());
                }
                long dbBookImageUploadToS3Start = System.currentTimeMillis();
                amazonS3.putObject(bucket, fileUrl, file);
                long dbBookImageUploadToS3End = System.currentTimeMillis();
                long dbBookImageUploadToS3TimeElapsed = dbBookImageUploadToS3End - dbBookImageUploadToS3Start;
                statsd.recordExecutionTime("uploadImageToS3Time", dbBookImageUploadToS3TimeElapsed);
                System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>" + picData + "<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<");
                UserPic userPicDetails = imageRepository.save(picData);
                long end = System.currentTimeMillis();
                long timeElapsed = end - start;
                statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
                logger.info("**********Image uploaded to S3 bucket successfully**********");
                return new ResponseEntity<>(userPicDetails, HttpStatus.CREATED);
            } catch (Exception e) {
                e.printStackTrace();
                logger.error("error [" + e.getMessage() + "] occurred while uploading Image ");
                long end = System.currentTimeMillis();
                long timeElapsed = end - start;
                statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
                logger.info("**********Error uploading image to S3**********");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }
        } else {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
    }

    private String generateFileName() {
        return new Date().getTime() + "-image.jpeg";
    }

    @GetMapping("/v1/user/self/pic")
    public ResponseEntity getPic(Authentication authentication){
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
        userRepository.flush();
        if (user.isVerified()) {
            statsd.incrementCounter("GetUserPicAPI");
            long start = System.currentTimeMillis();
            try {
                UserPic picData = imageRepository.findByUserId(user.getId().toString());
                if (picData != null) {
                    long end = System.currentTimeMillis();
                    long timeElapsed = end - start;
                    statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
                    logger.info("**********Image Retrieved from S3 bucket successfully**********");
                    return new ResponseEntity<>(picData, HttpStatus.OK);
                }
                long end = System.currentTimeMillis();
                long timeElapsed = end - start;
                statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
                logger.info("**********Image Not Found in S3 bucket **********");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                long end = System.currentTimeMillis();
                long timeElapsed = end - start;
                statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
                logger.info("**********Error while Retrieving Image from S3 bucket **********");
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
    }

//    @GetMapping("/v1/user/self/pic")
//    public ResponseEntity getPic(Authentication authentication){
//        User user = userRepository.findByUserName(authentication.getName())
//                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
//        userRepository.flush();
//        if (user.isVerified()) {
//            statsd.incrementCounter("GetUserPicAPI");
//            long start = System.currentTimeMillis();
//            try {
//                Session session = DAO.getSessionFactoryReplica().openSession();
//                logger.info("**********Pic Session initialized !**********");
//                List<UserPic> result = session.createQuery("from UserPic ").list();
//                logger.info("**********UserPic create query result !**********");
//                logger.info("**********session transaction commit !**********");
//                long end = System.currentTimeMillis();
//                long dbTimeElapsed = end - start;
//                long timeElapsed = end - start;
//                statsd.recordExecutionTime("GetUserPicFromDBTime", dbTimeElapsed);
//                session.close();
//                logger.info("**********session closed !**********");
//                statsd.recordExecutionTime("GetUserPicDetailsApiTime", timeElapsed);
//                logger.info("**********User details fetched successfully !**********");
//                for (UserPic userPic : result) {
//                    if (userPic.getUser_id().equalsIgnoreCase(user.getId().toString())) {
//                        statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
//                        logger.info("**********Image Retrieved from S3 bucket successfully**********");
//                        return new ResponseEntity<>(userPic, HttpStatus.OK);
//                    }
//                }
//                end = System.currentTimeMillis();
//                timeElapsed = end - start;
//                statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
//                logger.info("**********Image Not Found in S3 bucket **********");
//                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
//            } catch (Exception e) {
//                long end = System.currentTimeMillis();
//                long timeElapsed = end - start;
//                statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
//                logger.info("**********Error while Retrieving Image from S3 bucket **********");
//                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
//            }
//        } else {
//            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
//        }
//    }

    @DeleteMapping("/v1/user/self/pic")
    public ResponseEntity deletePic(Authentication authentication) {
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
        userRepository.flush();
        if (user.isVerified()) {
            statsd.incrementCounter("DeleteUserPicAPI");
            long start = System.currentTimeMillis();
            try {
                UserPic imageData = imageRepository.findByUserId(user.getId().toString());
                if (imageData != null) {
                    logger.info(">>>>>>>>>>>>>>>>>>> Delete - user - " + imageData.getUser_id());
                    long dbBookImageDeleteFromS3Start = System.currentTimeMillis();
                    amazonS3.deleteObject(bucket, imageData.getUrl());
                    long dbBookImageDeleteFromS3End = System.currentTimeMillis();
                    long dbBookImageDeleteFromS3TimeElapsed = dbBookImageDeleteFromS3End - dbBookImageDeleteFromS3Start;
                    statsd.recordExecutionTime("uploadImageToS3Time", dbBookImageDeleteFromS3TimeElapsed);
                    imageRepository.deleteByUserId(imageData.getUser_id());
                    long end = System.currentTimeMillis();
                    long timeElapsed = end - start;
                    statsd.recordExecutionTime("DeleteImageFromS3ApiTime", timeElapsed);
                    logger.info("**********Image Deleted from S3 bucket successfully**********");
                    return new ResponseEntity<>(HttpStatus.OK);
                }
                long end = System.currentTimeMillis();
                long timeElapsed = end - start;
                statsd.recordExecutionTime("DeleteImageFromS3ApiTime", timeElapsed);
                logger.info("**********Image Not Found in S3 bucket **********");
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            } catch (Exception e) {
                long end = System.currentTimeMillis();
                long timeElapsed = end - start;
                statsd.recordExecutionTime("DeleteImageFromS3ApiTime", timeElapsed);
                logger.info("**********Error while Deleting Image from S3 bucket **********");
                e.printStackTrace();
                return new ResponseEntity<>(null, HttpStatus.BAD_REQUEST);
            }
        } else {
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }
    }

}

