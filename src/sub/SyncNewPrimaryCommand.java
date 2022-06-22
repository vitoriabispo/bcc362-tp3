package sync;

import java.util.Set;
import java.util.SortedSet;
import core.Message;
import core.MessageImpl;
import core.PubSubCommand;

public class SyncNewPrimaryCommand implements PubSubCommand{

    @Override
    public Message execute(Message m, SortedSet<Message> log, Set<String> subscribers, boolean isPrimary,
                           String sencondaryServerAddress, int secondaryServerPort) {

        Message response = new MessageImpl();

        response.setLogId(m.getLogId()+1);
        System.out.println(m.getContent());
        response.setType("sync_primary_ack");

        return response;
    }

}