package agents;

import jade.core.Agent;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.FailureException;
import jade.domain.FIPAAgentManagement.NotUnderstoodException;
import jade.domain.FIPAAgentManagement.RefuseException;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.domain.FIPANames;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.proto.ContractNetResponder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 * @author Rodrigo Martínez
 */
public class BookSellerAgent extends Agent {

    Map<String, Integer> myCatalog;

    @Override
    protected void setup() {
        //Readig input arguments
        Object[] args = getArguments();
        if (args.length > 0) {
            myCatalog = new HashMap();
            for (int i = 0; i < args.length; i++) {
                myCatalog.put((String) args[i], Integer.valueOf((String) args[i + 1]));
                i++;
            }
            //Registering services on the DF
            register(generateServiceSet(myCatalog));
        }

        System.out.println("Agent " + getLocalName() + " catalog: ");
        System.out.println(myCatalog);

        System.out.println("Agent " + getLocalName() + ": Waiting for CFP...");
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));

        addBehaviour(new ContractNetResponder(this, template) {

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
                System.out.println("Agent " + getLocalName() + ": CPF received from " + cfp.getSender().getName() + "...");
                //The message must contain the name of the book needed
                if (myCatalog.containsKey(cfp.getContent())) {
                    //Making a proposal
                    System.out.println("Agent " + getLocalName() + ": Proposing...");
                    ACLMessage propose = cfp.createReply();
                    propose.setPerformative(ACLMessage.PROPOSE);
                    propose.setContent(String.valueOf(myCatalog.get(cfp.getContent())));
                    return propose;
                } else {
                    //Refusing to make a proposal
                    System.out.println("Agent " + getLocalName() + ": Refuse");
                    ACLMessage refuse = cfp.createReply();
                    refuse.setPerformative(ACLMessage.REFUSE);
                    return refuse;
//                    throw new RefuseException("evaluation-failed");
                }
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                //If the book is still in the catalog
                if (myCatalog.containsKey(accept.getContent())) {
                    ACLMessage inform = accept.createReply();
                    inform.setPerformative(ACLMessage.INFORM);
                    inform.setContent("Thanks for buying :D");
                    //The is eliminated from the catalog
                    myCatalog.remove(accept.getContent());
                    System.out.println("Agent " + getLocalName() + ": My proposal was accepted");
                    //Deregistering the service (book) from DF
                    register(generateServiceSet(myCatalog));

                    System.out.println("Agent " + getLocalName() + ": actualized catalog -> \n" + myCatalog);

                    return inform;
                } else {
                    ACLMessage failure = accept.createReply();
                    failure.setPerformative(ACLMessage.INFORM);
                    failure.setContent("Not available");
                    return failure;
                }
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                System.out.println("Agent " + getLocalName() + ": My proposal was rejected...");
            }
        });
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

    private void register(Set<ServiceDescription> services) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        for (ServiceDescription sd : services) {
            dfd.addServices(sd);
        }
        try {
            if (DFService.search(this, dfd).length > 0) {//If already registered
                DFService.deregister(this, dfd);
            }

            DFService.register(this, dfd);
            System.out.println("Agent " + getLocalName() + ": Registering services on the DF");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void deregister() {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        try {
            DFAgentDescription[] list = DFService.search(this, dfd);
            if (DFService.search(this, dfd).length == 0) {
                System.out.println("Agent " + getLocalName() + ": Deregistering services from DF...");
                DFService.register(this, dfd);
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private Set<ServiceDescription> generateServiceSet(Map<String, Integer> catalog) {
        Set<ServiceDescription> services = new HashSet();
        ServiceDescription sd;
        for (String service : catalog.keySet()) {
            sd = new ServiceDescription();
            sd.setType("book-selling");
            sd.setName(service);
            services.add(sd);
        }
        return services;
    }
}
