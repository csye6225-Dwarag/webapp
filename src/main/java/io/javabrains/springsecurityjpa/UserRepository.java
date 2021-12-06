package io.javabrains.springsecurityjpa;

import io.javabrains.springsecurityjpa.models.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByUserName(String userName);

//    @Modifying
//    @Query("UPDATE users SET c.address = :address WHERE c.id = :companyId")
//    int updateAddress(@Param("companyId") int companyId, @Param("address") String address);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User SET firstName= :fName, lastName= :lName, password=:password, accountUpdated=:accountUpdated WHERE userName in :email")
    public int updateUser(@Param(value = "email") String email, @Param("fName") String fName, @Param("lName") String LName,
                          @Param("password") String password, @Param("accountUpdated") Timestamp accountUpdated);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User SET firstName= :fName, accountUpdated=:accountUpdated WHERE userName in :email")
    public int updateUserFirstName(@Param(value = "email") String email, @Param("fName") String fName, @Param("accountUpdated") Timestamp accountUpdated);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User SET lastName= :lName, accountUpdated=:accountUpdated WHERE userName in :email")
    public int updateUserLastName(@Param(value = "email") String email, @Param("lName") String lName, @Param("accountUpdated") Timestamp accountUpdated);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User SET password=:password, accountUpdated=:accountUpdated WHERE userName in :email")
    public int updateUserPassword(@Param(value = "email") String email, @Param("password") String password, @Param("accountUpdated") Timestamp accountUpdated);

    @Transactional
    @Modifying(clearAutomatically = true)
    @Query("UPDATE User SET verified= :verified, verifiedOn= :verifiedOn, accountUpdated=:accountUpdated WHERE userName in :email")
    public int updateUserVerified(@Param(value = "email") String email, @Param("verified") boolean verified, @Param("verifiedOn") Timestamp verifiedOn, @Param("accountUpdated") Timestamp accountUpdated);
}
