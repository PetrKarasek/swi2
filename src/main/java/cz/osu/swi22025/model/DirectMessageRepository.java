package cz.osu.swi22025.model;

import cz.osu.swi22025.model.DirectMessage;
import cz.osu.swi22025.model.ChatUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DirectMessageRepository extends JpaRepository<DirectMessage, Integer> {
    
    @Query("SELECT dm FROM DirectMessage dm WHERE " +
           "(dm.sender = :user1 AND dm.receiver = :user2) OR " +
           "(dm.sender = :user2 AND dm.receiver = :user1) " +
           "ORDER BY dm.sendTime ASC")
    List<DirectMessage> findConversationBetweenUsers(@Param("user1") ChatUser user1, 
                                                    @Param("user2") ChatUser user2);
    
    List<DirectMessage> findByReceiverOrderBySendTimeDesc(ChatUser receiver);
    
    List<DirectMessage> findBySenderOrderBySendTimeDesc(ChatUser sender);
    
    @Query("SELECT dm FROM DirectMessage dm WHERE dm.receiver = :receiver AND dm.isRead = false ORDER BY dm.sendTime ASC")
    List<DirectMessage> findUnreadMessages(@Param("receiver") ChatUser receiver);
}
