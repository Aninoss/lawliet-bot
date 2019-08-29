package General;

import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import General.RunningCommands.RunningCommand;
import General.RunningCommands.RunningCommandManager;
import ServerStuff.SIGNALTRANSMITTER.SIGNALTRANSMITTER;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class Console {
    public static void manageConsole() {
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        while(true) {
            try {
                if (br.ready()) {
                    String s = br.readLine();
                    if (s != null) {
                        switch (s) {
                            case "quit":
                                System.exit(0);
                                break;

                            case "threads":
                                System.out.println(Thread.getAllStackTraces().keySet().size());
                                break;

                            case "activities":
                                System.out.println("----------------------------------------");
                                for (int i = 0; i < 10; i++) {
                                    if (CommandContainer.getInstance().getReactionInstances().size() < i + 1) break;
                                    Command command = CommandContainer.getInstance().getReactionInstances().get(i);
                                    System.out.println(command.getTrigger());
                                }
                                int reactionN = CommandContainer.getInstance().getReactionInstances().size();
                                System.out.println("----------------------------------------");
                                System.out.println("Reaction Listener Activities: " + reactionN);
                                System.out.println("----------------------------------------");

                                for (int i = 0; i < 10; i++) {
                                    if (CommandContainer.getInstance().getMessageForwardInstances().size() < i + 1)
                                        break;
                                    Command command = CommandContainer.getInstance().getMessageForwardInstances().get(i);
                                    System.out.println(command.getTrigger());
                                }
                                int forwardN = CommandContainer.getInstance().getMessageForwardInstances().size();
                                System.out.println("----------------------------------------");
                                System.out.println("Forwarded Message Listener Activities: " + forwardN);
                                System.out.println("----------------------------------------");

                                break;

                            case "commands":
                                System.out.println("----------------------------------------");

                                ArrayList<RunningCommand> commands = RunningCommandManager.getInstance().getRunningCommands();
                                for (RunningCommand command : commands) {
                                    System.out.println(command.getCommandTrigger());
                                }

                                System.out.println("----------------------------------------");
                                System.out.println("Nicht abgeschlossene Commands: " + commands.size());
                                System.out.println("----------------------------------------");

                                break;

                            case "clearcommands":
                                RunningCommandManager.getInstance().clear();
                                System.out.println("ok");

                                break;

                            case "traffic":
                                System.out.println(SIGNALTRANSMITTER.getInstance().getTrafficGB());
                                break;

                            case "memory":
                                System.out.println("Free Memory: " + (Runtime.getRuntime().freeMemory() / (1024 * 1024)) + " MB" );
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}