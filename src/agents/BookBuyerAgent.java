package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.proto.ContractNetInitiator;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The name of the book to buy and the money available must be passed as
 * arguments when the agent is created.
 *
 * @author Rodrigo Martínez
 */
public class BookBuyerAgent extends Agent {

    String bookToBuy;
    int moneyAvailable;
    Set<AID> sellersAID;
    ACLMessage firstMessage;

    @Override
    protected void setup() {
        //The input arguments (service or book the agent is searching) are read
        Object[] args = getArguments();
        bookToBuy = (String) args[0];
        moneyAvailable = Integer.valueOf((String) args[1]);

        System.out.println("Agent " + getLocalName() + ": I want to buy " + bookToBuy);

        //Register his buyer service on the DF
        ServiceDescription sd = new ServiceDescription();
        sd.setType("book-buying");
        sd.setName(bookToBuy);
        register(sd);

        //Search for a service (book) on the DF
        DFAgentDescription dfd = new DFAgentDescription();
        sd = new ServiceDescription();
        sd.setType("book-selling");
        sd.setName(bookToBuy);
        dfd.addServices(sd);
        try {
            DFAgentDescription[] result = DFService.search(this, dfd);
            if (result.length > 0) {
                //Creating the CFP Message
                firstMessage = new ACLMessage(ACLMessage.CFP);
                firstMessage.setProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET);
                firstMessage.setReplyByDate(new Date(System.currentTimeMillis() + 10000));
                firstMessage.setContent(bookToBuy);

                //Adding receivers to the message
                sellersAID = new HashSet();
                for (DFAgentDescription e : result) {
                    firstMessage.addReceiver(e.getName());
                    sellersAID.add(e.getName());
                }
            } else {
                System.out.println("Agent " + getLocalName() + ": Nobody has the book I wan't :c");
                doDelete();//We close the agent
            }
        } catch (FIPAException ex) {
            Logger.getLogger(BookBuyerAgent.class.getName()).log(Level.SEVERE, null, ex);
        }

        addBehaviour(new ContractNetInitiator(this, firstMessage) {

            @Override
            protected void handlePropose(ACLMessage propose, Vector acceptances) {
                System.out.println("Agent " + getLocalName() + ": propose message received from " + propose.getSender().getLocalName() + "-> " + propose.getContent());
            }

            @Override
            protected void handleFailure(ACLMessage failure) {
                System.out.println("Agent " + getLocalName() + ": failure message received from " + failure.getSender().getLocalName());
            }

            @Override
            protected void handleInform(ACLMessage inform) {
                System.out.println("Agent " + getLocalName() + ": inform message received from " + inform.getSender().getLocalName());
            }

            @Override
            protected void handleRefuse(ACLMessage refuse) {
                System.out.println("Agent " + getLocalName() + ": refuse message received from " + refuse.getSender().getLocalName());

            }

            @Override
            protected void handleAllResponses(Vector responses, Vector acceptances) {
                System.out.println("Agent " + getLocalName() + ": " + responses.size() + " proposals recieved");
                if (responses.size() < sellersAID.size()) {
                    System.out.println("Timeout expired: missing " + (sellersAID.size() - responses.size()) + " responses");
                }
                //Evaluating proposals considering our moneyAvailable
                int bestProposal = 1000000;//This number must big enough
                AID bestProposer = null;
                ACLMessage accept = null;

                for (Object a : responses) {
                    ACLMessage msg = (ACLMessage) a;
                    if (msg.getPerformative() == ACLMessage.PROPOSE) {
                        ACLMessage reply = msg.createReply();
                        reply.setPerformative(ACLMessage.REJECT_PROPOSAL);
                        acceptances.add(reply);
                        int proposal = Integer.valueOf(msg.getContent());
                        if (proposal < bestProposal && proposal <= moneyAvailable) {
                            bestProposal = proposal;
                            bestProposer = msg.getSender();
                            accept = reply;
                        }
                    }
                }
                //Accepting the proposal of the best proposer
                if (accept != null) {
                    System.out.println("Accepting proposal " + bestProposal + " from responder " + bestProposer.getLocalName());
                    accept.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                }
            }

            @Override
            protected void handleAllResultNotifications(Vector resultNotifications) {

            }
        }
        );

    }

    @Override
    protected void takeDown() {
        System.out.println("Agent " + getLocalName() + " is closing...");
        //Deregistering services from DF
        deregister();
    }

    private void register(ServiceDescription sd) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
            DFAgentDescription[] list = DFService.search(this, dfd);
            if (list.length > 0) {
                DFService.deregister(this);
            }
            dfd.addServices(sd);
            System.out.println("Agent " + getLocalName() + ": Registering service on the DF");
            DFService.register(this, dfd);

        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void deregister() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
            DFAgentDescription[] list = DFService.search(this, dfd);
            if (DFService.search(this, dfd).length != 0) {
                System.out.println("Agent " + getLocalName() + ": Deregistering services from DF...");
                DFService.deregister(this, dfd);
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
