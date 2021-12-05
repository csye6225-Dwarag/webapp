package io.javabrains.springsecurityjpa;

import io.javabrains.springsecurityjpa.models.User;
import io.javabrains.springsecurityjpa.models.UserPic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ImageRepository extends JpaRepository<UserPic, Integer>
{
    public UserPic findByUserId(String userId);

    @Transactional
    @Query("SELECT u FROM UserPic u WHERE u.userId = :userId")
    public UserPic getUserData(@Param(value = "userId") String userId);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE UserPic SET file_name= :fileName, url= :url, upload_date=:uploadDate WHERE userId in :userId")
    public int updatePic(@Param(value = "userId") String userId,@Param(value = "fileName") String fileName,@Param(value = "url") String url,@Param(value = "uploadDate") Timestamp uploadDate);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM UserPic WHERE userId =:userId")
    public int deleteByUserId(@Param(value = "userId") String userId);

    //Object updatePic(String userID, String fileName, String url, Timestamp uploadDate);
}
