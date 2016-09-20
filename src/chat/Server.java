package chat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Server {
	  /*
     * ServerSocket是运行在服务端的Socket
     * 它的作用是：
     * 1：申请服务端口，客户端就是通过该端口连接到服务端的。
     * 2：坚挺申请的服务端口，一旦客户端连接后，就创建一个
     * Socket实力与该客户端交互。
     */
	private ServerSocket server;
	private Map<String,PrintWriter> allOut;
	public Server() throws Exception{
		try{
			/*
			 * 初始化ServerSocket并指定服务端口
			 * 该端口不能与其他应用程序冲突，否则
			 * 会抛出异常。
			 */
			server = new ServerSocket(8088);
			allOut = new HashMap<String,PrintWriter>();
		}catch(Exception e){
            throw e;			
		}
	}
	/**
	 * 向共享级和中添加给定的客户端输出流
	 * @param out
	 */
	private synchronized void addOut(String nic,PrintWriter out){
		allOut.put(nic,out);
	}
	/**
	 *  将给定的客户端的输出流从共享集合中删除。
	 * @param out
	 */
	private synchronized void removeOut(String nic,PrintWriter out){
		allOut.remove(nic,out);
	}
	/**
	 * 将给定的消息发送给所有用户
	 * @param message
	 */
	private synchronized void sendMessage(String message){
		for(PrintWriter out: allOut.values()){
			out.println(message);
		}
	}
	private synchronized PrintWriter get(String host){
		return allOut.get(host);
		
	}
	//服务端开始工作的方法
	public void start(){
    	try{
    		/*
    		 * Socket accept()
    		 * SeverSocket提供了accept方法，该方法用于监听申请的服务端口，直到
    		 * 一个客户端连接然后返回一个Socket实例用于与该客户端通讯。
    		 * 这个方法是一个阻塞方法，直到客户端连接返回继续向下执行。
    		 */
    		while(true){
    		System.out.println("等待客户端连接...");
    		Socket socket = server.accept();
    		System.out.println("一个客户端连接了！");
    		
    		ClientHandler handler=new ClientHandler(socket);
    		Thread t = new Thread(handler);
    		t.start();
    		}
    	}catch(Exception e){
    		e.printStackTrace();
    	}
	}
	public static void main(String[] args) {
		try {
			Server server =new Server();
			server.start();
		}catch (Exception e){
			e.printStackTrace();
		}
	}
	/**
	 * 该线程的任务是负责与指定的客户端进行交互
	 * @author zhengruikai
	 *
	 */
class ClientHandler implements Runnable{
	private Socket socket;
	private String host;
	//客户端地址信息
	
	public ClientHandler(Socket socket){
		this.socket=socket;
		/*
	 * 通过Socket可以获取远程计算机得知信息，对于服务端而言，
	 * 远程计算机即客户端。
	 */
		InetAddress address= socket.getInetAddress();
	    //获取IP地址的字符串形式
		host = address.getHostAddress();
	}
	public void run(){
		PrintWriter pw =null;
		try{
			sendMessage("["+host+"]上线了！");
			OutputStream out = socket.getOutputStream();
			OutputStreamWriter osw 
			= new OutputStreamWriter(out,"UTF-8");
		    pw
			=new PrintWriter(osw,true);
			//将该客户端的输出流存入共享集合
			addOut(host,pw);
    		/*
    		 * InputStream getInputStream()
    		 * Socket提供的该方法用来获取一个输入流，通过该输入流可以读取远端
    		 * 计算机发送过来的数据。
    		 */
    		InputStream in
    		=socket.getInputStream();
    		InputStreamReader isr= new InputStreamReader(in,"UTF-8");
    		BufferedReader br = new BufferedReader(isr);
    		String message=null;
    		/*
    		 * 服务端接收客户端发送过来的消息是，由于客户端操作系统不同，那么当客户端
    		 * 断开连接时的效果也不相同。
    		 * 当windows的客户端断开连接后，br.readLine方法会抛出异常。
    		 * 当linux的客户端断开连接后，br.readLine会返回null。
    		 */
    		
    		while((message=br.readLine())!=null){
    	
    		System.out.println(host+":"+message);
    		
    		if(message.startsWith("@")){
    		int indexe=message.indexOf(":");
    			String dest=message.substring(1, indexe);
    		PrintWriter pw1 = get(dest);
    		pw1.println(message);
    		pw1.flush();
    		}else{
    			sendMessage(host+":"+message);
    		}
    		}
		}catch(Exception e){
			
		}finally{
			/*
			 * 客户端与服务端断开后
			 */
			try{
				//将该客户端的输出流从共享集合中删除
				removeOut(host,pw);
				sendMessage("["+host+"]下线了！");
				socket.close();
				
			}catch(IOException e){
				e.printStackTrace();
			}
		}
	}
}
}
