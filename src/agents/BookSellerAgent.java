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
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Rodrigo Martínez
 */
public class BookSellerAgent extends Agent {

    @Override
    protected void setup() {
        //Readig input arguments
        Object[] args = getArguments();
        if (args.length > 0) {
            
            DFAgentDescription dfd = new DFAgentDescription();
            dfd.setName(getAID());
            ServiceDescription sd = new ServiceDescription();
            sd.setType("book-selling");
            sd.setName("");
            
            for (int i = 0; i < args.length; i++) {
                sd.setName((String) args);
                String serviceName = (String) args[i];
                s
                i++;
            }

        }

        System.out.println("Agent " + getLocalName() + ": Waiting for CFP...");
        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_CONTRACT_NET),
                MessageTemplate.MatchPerformative(ACLMessage.CFP));

        addBehaviour(new ContractNetResponder(this, template) {

            @Override
            protected ACLMessage handleCfp(ACLMessage cfp) throws RefuseException, FailureException, NotUnderstoodException {
                System.out.println("Agent " + getLocalName() + ": CPF received from " + cfp.getSender().getName() + "...");
                if (evaluateProposal(cfp)) {
                    //Making a proposal
                    System.out.println("Agent " + getLocalName() + ": Proposing");//Include the price

                } else {
                    //Refusing to make a proposal
                    System.out.println("Agent " + getLocalName() + ": Refuse");
                    throw new RefuseException("evaluation-failed");
                }

                return propose;
            }

            @Override
            protected ACLMessage handleAcceptProposal(ACLMessage cfp, ACLMessage propose, ACLMessage accept) throws FailureException {
                return super.handleAcceptProposal(cfp, propose, accept); //To change body of generated methods, choose Tools | Templates.
            }

            @Override
            protected void handleRejectProposal(ACLMessage cfp, ACLMessage propose, ACLMessage reject) {
                super.handleRejectProposal(cfp, propose, reject); //To change body of generated methods, choose Tools | Templates.
            }
        }
        );

        super.setup(); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    protected void takeDown() {
        super.takeDown(); //To change body of generated methods, choose Tools | Templates.
    }

    private boolean evaluateProposal(ACLMessage cfp) {
        return true;
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
            if (list.length == 0) {
                System.out.println("Agent " + getLocalName() + ": Deregistering services from DF...");
                DFService.register(this, dfd);
            }
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }
}
