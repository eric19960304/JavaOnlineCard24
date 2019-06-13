import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.ArrayList;

public interface Auth extends Remote {
	ArrayList<Player> login(String userName, String password) throws RemoteException;
	ArrayList<Player> register(String userName, String password) throws RemoteException;
	void logout(String userName) throws RemoteException;
}