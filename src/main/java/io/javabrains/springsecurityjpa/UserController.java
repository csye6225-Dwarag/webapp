package io.javabrains.springsecurityjpa;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
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
import org.springframework.web.multipart.MultipartFile;
import com.timgroup.statsd.StatsDClient;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@RestController
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    ImageRepository imageRepository;

//    @Autowired
//    UserReadOnlyRepository userReadOnlyRepository;

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
    //private String bucketURL="https://s3.console.aws.amazon.com/s3/buckets/csye6225.prod.domain.tld?region=us-east-1&tab=objects";

//    private String bucket = "csye6225.prod.domain.tld";
//    private String bucketURL = "https://s3.console.aws.amazon.com/s3/buckets/csye6225.prod.domain.tld?region=us-east-1&tab=objects";

//    @GetMapping("/")
//    public String home() {
//        return ("<h1>Welcome</h1>");
//    }

    @GetMapping("/v1/user/self")
    public ResponseEntity<User> user(Authentication authentication) {
        statsd.incrementCounter("GetUserDetailsApi");
        long start = System.currentTimeMillis();
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
        long end = System.currentTimeMillis();
        long dbTimeElapsed = end - start;
        long timeElapsed = end - start;
        statsd.recordExecutionTime("GetUserFromDBTime", dbTimeElapsed);
        statsd.recordExecutionTime("GetUserDetailsApiTime", timeElapsed);
        logger.info("**********User details fetched successfully !**********");
        if(user.isVerified()){
            return ResponseEntity.ok(user);
        }else{
            return new ResponseEntity<>(null, HttpStatus.FORBIDDEN);
        }

    }

    // build update user REST API
    @PutMapping("/v1/user/self")
    public ResponseEntity<User> updateUser(Authentication authentication, @RequestBody User userDetails) {
        try {
            statsd.incrementCounter("UpdateUserDetailsAPI");
            long start = System.currentTimeMillis();
            if (userDetails.getUserName() != null && !userDetails.getUserName().isEmpty()) {
                logger.info("**********Cannot Update email ! **********");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            if (userDetails.getAccountCreated() != null) {
                logger.info("**********Cannot Update Account Created details ! **********");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            if (userDetails.getAccountUpdated() != null) {
                logger.info("**********Cannot Update Account Updated details ! **********");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
            }

            if ((userDetails.getFirstName() != null && !userDetails.getFirstName().isEmpty())
                    && (userDetails.getLastName() != null && !userDetails.getLastName().isEmpty()) &&
                    (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty())) {
                String password = userDetails.getPassword();
                BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                userDetails.setPassword(bCryptPasswordEncoder.encode(password));
                userDetails.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
                //System.out.println(">>>>>>>>>>>>>>>>>Pass -" + userDetails.getPassword());
                long dbStart = System.currentTimeMillis();
                userRepository.updateUser(authentication.getName(), userDetails.getFirstName(),
                        userDetails.getLastName(), userDetails.getPassword(), userDetails.getAccountUpdated());
                long end = System.currentTimeMillis();
                long dbTimeElapsed = end - dbStart;
                long timeElapsed = end - start;
                statsd.recordExecutionTime("saveUserToDBTime", dbTimeElapsed);
                statsd.recordExecutionTime("createNewUserApiTime", timeElapsed);
                logger.info("**********Creating New User**********");
                return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
            }

            if (userDetails.getFirstName() != null && !userDetails.getFirstName().isEmpty()) {
                userDetails.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
                userRepository.updateUserFirstName(authentication.getName(), userDetails.getFirstName(), userDetails.getAccountUpdated());
            }
            if (userDetails.getLastName() != null && !userDetails.getLastName().isEmpty()) {
                userDetails.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
                userRepository.updateUserLastName(authentication.getName(), userDetails.getLastName(), userDetails.getAccountUpdated());
            }

            if (userDetails.getPassword() != null && !userDetails.getPassword().isEmpty()) {
                String password = userDetails.getPassword();
                BCryptPasswordEncoder bCryptPasswordEncoder = new BCryptPasswordEncoder();
                userDetails.setPassword(bCryptPasswordEncoder.encode(password));
                //System.out.println(">>>>>>>>>>>>>>>>>Pass -" + userDetails.getPassword());
                userRepository.updateUserPassword(authentication.getName(), userDetails.getPassword(), userDetails.getAccountUpdated());
            }
            return ResponseEntity.status(HttpStatus.NO_CONTENT).body(null);
        } catch (Exception exception) {
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
            Instant expirationInstant = currentInstant.plusSeconds(300);

            expirationTTL = expirationInstant.getEpochSecond();
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
            newUser.setVerifiedOn(new Timestamp(System.currentTimeMillis()));
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

    public ResponseEntity<String> verifyUserEmail(@RequestParam("email") String header_email, @RequestParam("token") String header_token ) {
        try{
            dynamodbClient = AmazonDynamoDBClientBuilder.defaultClient();
            /* Create an Object of GetItemRequest */
            GetItemRequest request = new GetItemRequest();

            /* Setting Table Name */
            request.setTableName("csye6225-dynamo");

            /* Setting Consumed Capacity */
            request.setReturnConsumedCapacity(ReturnConsumedCapacity.TOTAL);

            /* Setting Consistency Models */
            /* true for Strong Consistent & false for Eventually Consistent */
            request.setConsistentRead(true);

            /* Create a Map of Primary Key attributes */
            Map<String, AttributeValue> keysMap = new HashMap<>();
            keysMap.put("Email", new AttributeValue(header_email));

            request.setKey(keysMap);
            logger.info("**********DynamoDB before get**********");
            GetItemResult result = dynamodbClient.getItem(request);
            logger.info("**********DynamoDB after get success**********");
            AtomicReference<Boolean> check1 = new AtomicReference<>(false);
            AtomicReference<Boolean> check2 = new AtomicReference<>(false);
            if (result.getItem() != null) {
                result.getItem().entrySet().stream()
                        .forEach(e -> {
                            if(e.getKey() == "AccessToken"){
                                if(e.getValue().toString() == header_token){
                                    check1.set(true);
                                    logger.info("**********check 1**********" + check1.get().toString());

                                }
                            }
                            if(e.getKey() == "TTL"){
                                if(Long.parseLong(e.getValue().toString()) >= Long.parseLong(String.valueOf(Instant.now()))){
                                    check2.set(true);
                                    logger.info("**********check 2**********" + check2.get().toString());

                                }
                            }
                        });

            }
            logger.info(check1.get().toString() + "<<<<<<<<<<<<<<<<<<<<>>>>>>>>>>>>>>>>>>>>>>" + check2.get().toString());
            if(check1.get() && check2.get()){

                return new ResponseEntity<>(null,HttpStatus.OK);
            }
            return new ResponseEntity<>(null,HttpStatus.NOT_FOUND);
        }
        catch (Exception e){
            logger.info("**********Exception!**********");
            logger.error(e.toString());
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
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
            newUser.setAccountCreated(new Timestamp(System.currentTimeMillis()));
            newUser.setAccountUpdated(new Timestamp(System.currentTimeMillis()));
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

            User user = userRepository.findByUserName(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
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
        }catch (Exception e){
            e.printStackTrace();
            logger.error("error [" + e.getMessage() + "] occurred while uploading Image ");
            long end = System.currentTimeMillis();
            long timeElapsed = end - start;
            statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
            logger.info("**********Error uploading image to S3**********");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
    }

    private String generateFileName() {
        return new Date().getTime() + "-image.jpeg";
    }

    @GetMapping("/v1/user/self/pic")
    public ResponseEntity getPic(Authentication authentication){
        statsd.incrementCounter("GetUserPicAPI");
        long start = System.currentTimeMillis();
        try{
            User user = userRepository.findByUserName(authentication.getName())
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
            UserPic picData = imageRepository.findByUserId(user.getId().toString());
            if(picData != null) {
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
        }
        catch (Exception e){
            long end = System.currentTimeMillis();
            long timeElapsed = end - start;
            statsd.recordExecutionTime("insertImageToS3ApiTime", timeElapsed);
            logger.info("**********Error while Retrieving Image from S3 bucket **********");
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
    }

    @DeleteMapping("/v1/user/self/pic")
    public ResponseEntity deletePic(Authentication authentication){
        statsd.incrementCounter("DeleteUserPicAPI");
        long start = System.currentTimeMillis();
        User user = userRepository.findByUserName(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Employee not exist with id:" + authentication.getName()));
        // UserPic picData = imageRepository.findByUserId(user.getId().toString());
        try{
            UserPic imageData = imageRepository.findByUserId(user.getId().toString());
            if (imageData != null) {
                System.out.println(">>>>>>>>>>>>>>>>>>> Delete - user - "+ imageData.getUser_id());
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
        }
        catch (Exception e){
            long end = System.currentTimeMillis();
            long timeElapsed = end - start;
            statsd.recordExecutionTime("DeleteImageFromS3ApiTime", timeElapsed);
            logger.info("**********Error while Deleting Image from S3 bucket **********");
            e.printStackTrace();
            return new ResponseEntity<>(null,HttpStatus.BAD_REQUEST);
        }
    }

}

