package mychess.ui;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.JPanel;

import mychess.function.Commication;
import mychess.function.Internet;
import mychess.function.Redo;
import mychess.function.Replay;
import mychess.util.Common;
import mychess.util.HasFinished;
import mychess.util.JudgeMove;


public class ChessBoard extends JPanel implements MouseListener,Runnable{
	/** serialVersionUID*/
	private static final long serialVersionUID = 1L;
	private int col;//现在棋子所处的列
	private int row;//现在棋子所处的行
	private boolean tip;//是否重绘的时候加上提示（也就是走棋的时候有标记）
	private boolean isSelected;//棋子是否被选中，决定下一步点击是移动棋子还是在选择棋子
	private int precol;//上一步棋子所处的列
	private int prerow;//上一步棋子所处的行
	private boolean yourTurn;//是不是到你的回合
	private int[][] data;//当前棋局的状态数组,用于重绘
	private Image[] pics;//加载象棋的图片
	private Redo rd;//悔棋的所有棋局列表
	private Internet internet;//数据服务器对象
	private boolean observer;//是否是观察者角色
	private boolean isRed;//是否是红方
	private boolean isRedo;//是否悔棋
	private Commication cc;//消息服务器对象
	private byte state=0;//状态0表示空闲, 1表示准备,2表示游戏开始,3表示游戏进行中,4表示游戏结束
	
	public ChessBoard(Image[] pics,Redo rd) {
		// TODO Auto-generated constructor stub
		this.pics=pics;
		this.rd=rd;
		internet=new Internet();//开启数据服务器，至于消息服务器需要手动提前开启
		//判断角色和获取棋局初始状态
		String message=internet.readMessage();
		String message_array=internet.readMessage();
		data=Common.String_to_Array(message_array);
		rd.add_one_step(data);
		if(message.endsWith("true")){
			isRed=true;
			state=1;
			yourTurn=true;
		}
		else if(message.endsWith("false")) state=2;
		else observer=true;
		
		addMouseListener(this);//监听鼠标操作
		
		Thread t=new Thread(this);//数据服务交互
		t.start();
		cc=new Commication(this);
		Thread t2=new Thread(cc);//消息服务交互
		t2.start();
	}
	
	private void drawLines(int row,int col,int width,int height,Graphics g) {
		//1/2 2/3 1/3
		int baseX=width*col/11;
		int baseY=height*row/12;
		int wlen=width/110;
		int hlen=height/120;
		int w4len=width/44;
		int h4len=height/48;
		
		if(col!=9){
			g.drawLine(baseX+wlen, baseY-hlen, baseX+wlen, baseY-h4len);
			g.drawLine(baseX+wlen, baseY-hlen, baseX+w4len, baseY-hlen);
		
			g.drawLine(baseX+wlen, baseY+hlen, baseX+w4len, baseY+hlen);
			g.drawLine(baseX+wlen, baseY+hlen,baseX+wlen,baseY+h4len);
		}
		
		if(col!=1){
			g.drawLine(baseX-wlen, baseY-hlen, baseX-wlen,baseY-h4len);
			g.drawLine(baseX-wlen, baseY-hlen, baseX-w4len, baseY-hlen);
		
			g.drawLine(baseX-wlen, baseY+hlen, baseX-w4len, baseY+hlen);
			g.drawLine(baseX-wlen, baseY+hlen, baseX-wlen, baseY+h4len);
		}
	}
	
	@Override
	protected void paintComponent(Graphics g) {
		// TODO Auto-generated method stub
		super.paintComponent(g);
		int height=getHeight();
		int width=getWidth();
		//先画数线9个，横线10
		g.drawLine(width/11, height/12, width/11, height*10/12);
		g.drawLine(width*9/11, height/12, width*9/11, height*10/12);
		for(int i=0;i<7;i++){
			g.drawLine(width*(i+2)/11, height/12, width*(i+2)/11, height*5/12);
			g.drawLine(width*(i+2)/11, height/2, width*(i+2)/11, height*10/12);
		}
		for(int i=0;i<10;i++){
			g.drawLine(width/11, height*(i+1)/12, width*9/11, height*(i+1)/12);
		}
		
		//画楚河汉界
		g.setFont(new Font("华文行楷", Font.BOLD, 30));
		g.drawString("楚        河", 200, 330);
		g.drawString("汉        界", 900, 330 );
		
		//画士的线
		g.drawLine(width*4/11, height/12, width*6/11, height/4);
		g.drawLine(width*6/11, height/12, width*4/11, height/4);
		g.drawLine(width*4/11, height*10/12, width*6/11, height*8/12);
		g.drawLine(width*6/11, height*10/12, width*4/11, height*8/12);
		
		drawLines(3, 2, width, height, g);
		drawLines(3, 8, width, height, g);
		drawLines(8, 2, width, height, g);
		drawLines(8, 8, width, height, g);//炮
		
		drawLines(4, 1, width, height, g);
		drawLines(4, 3, width, height, g);
		drawLines(4, 5, width, height, g);
		drawLines(4, 7, width, height, g);
		drawLines(4, 9, width, height, g);
		drawLines(7, 1, width, height, g);
		drawLines(7, 3, width, height, g);
		drawLines(7, 5, width, height, g);
		drawLines(7, 7, width, height, g);
		drawLines(7, 9, width, height, g);//兵
		
		//画图像
		for(int i=1;i<=10;i++){
			for(int j=1;j<=9;j++){
				if(data[i-1][j-1]==0) continue;
				int baseX=width*j/11;
				int baseY;
				if(!isRed)
					baseY=height*(11-i)/12;
				else
					baseY=height*i/12;
				g.drawImage(pics[data[i-1][j-1]], baseX-width/22, baseY-height/24, width/11, height/12, this);
			}
		}
		
		if(yourTurn && !isRedo && state==3){
			//画对方的提示
			g.setColor(Color.GREEN);
			g.drawLine(precol*width/11-width/22, prerow*height/12-height/24, precol*width/11-width/44, prerow*height/12-height/24);
			g.drawLine(precol*width/11-width/22, prerow*height/12-height/24, precol*width/11-width/22, prerow*height/12-height/48);
			g.drawLine(precol*width/11+width/22, prerow*height/12-height/24, precol*width/11+width/44, prerow*height/12-height/24);
			g.drawLine(precol*width/11+width/22, prerow*height/12-height/24, precol*width/11+width/22, prerow*height/12-height/48);
			g.drawLine(precol*width/11+width/22, prerow*height/12+height/24, precol*width/11+width/44, prerow*height/12+height/24);
			g.drawLine(precol*width/11+width/22, prerow*height/12+height/24, precol*width/11+width/22, prerow*height/12+height/48);
			g.drawLine(precol*width/11-width/22, prerow*height/12+height/24, precol*width/11-width/44, prerow*height/12+height/24);
			g.drawLine(precol*width/11-width/22, prerow*height/12+height/24, precol*width/11-width/22, prerow*height/12+height/48);
			g.setColor(Color.black);
			g.setColor(Color.BLUE);
			g.drawLine(col*width/11-width/22, row*height/12-height/24, col*width/11-width/44, row*height/12-height/24);
			g.drawLine(col*width/11-width/22, row*height/12-height/24, col*width/11-width/22, row*height/12-height/48);
			g.drawLine(col*width/11+width/22, row*height/12-height/24, col*width/11+width/44, row*height/12-height/24);
			g.drawLine(col*width/11+width/22, row*height/12-height/24, col*width/11+width/22, row*height/12-height/48);
			g.drawLine(col*width/11+width/22, row*height/12+height/24, col*width/11+width/44, row*height/12+height/24);
			g.drawLine(col*width/11+width/22, row*height/12+height/24, col*width/11+width/22, row*height/12+height/48);
			g.drawLine(col*width/11-width/22, row*height/12+height/24, col*width/11-width/44, row*height/12+height/24);
			g.drawLine(col*width/11-width/22, row*height/12+height/24, col*width/11-width/22, row*height/12+height/48);
			g.setColor(Color.black);
		}
		//画一个提示
		if(tip){
			g.setColor(Color.green);
			g.drawLine(col*width/11-width/22, row*height/12-height/24, col*width/11-width/44, row*height/12-height/24);
			g.drawLine(col*width/11-width/22, row*height/12-height/24, col*width/11-width/22, row*height/12-height/48);
			g.drawLine(col*width/11+width/22, row*height/12-height/24, col*width/11+width/44, row*height/12-height/24);
			g.drawLine(col*width/11+width/22, row*height/12-height/24, col*width/11+width/22, row*height/12-height/48);
			g.drawLine(col*width/11+width/22, row*height/12+height/24, col*width/11+width/44, row*height/12+height/24);
			g.drawLine(col*width/11+width/22, row*height/12+height/24, col*width/11+width/22, row*height/12+height/48);
			g.drawLine(col*width/11-width/22, row*height/12+height/24, col*width/11-width/44, row*height/12+height/24);
			g.drawLine(col*width/11-width/22, row*height/12+height/24, col*width/11-width/22, row*height/12+height/48);
			g.setColor(Color.black);
		}
	}
	
	//鼠标事件
	@Override
	public void mouseClicked(MouseEvent e) {
		// TODO Auto-generated method stub
		if(!yourTurn || observer || state==4) return;//如果不是自己回合或者角色为旁观者或者当前游戏状态已经结束
													//则无法点击棋盘
		//确定位置，获取现在棋子的列和行
		//如果isSelected为假，那么这个列col和行row将是下次isSelected为真的时候之前的preCol和preRow
		//如果isSelected为真，那么这个列col和行row将是现在的col和row
		int width=getWidth();
		int height=getHeight();
		int x=e.getX();
		int y=e.getY();
		for(int i=1;i<=9;i++){
			if(Math.abs(width*i/11-x)<width/22){
				col=i;
				break;
			}
		}
		for(int i=1;i<=10;i++){
			if(Math.abs(height*i/12-y)<height/24){
				row=i;
				break;
			}
		}
		
		if(isSelected){//如果已经选中了棋子，接下来就是移动棋子操作（此时四个坐标已经确定了）
			//判断能不能到
			//先判断是否被将军
			int[] aixs=new int[4];
			if(isRed){//调整坐标
				aixs=new int[]{prerow,precol,row,col};
			}else{
				aixs=new int[]{11-prerow,precol,11-row,col};
				prerow=11-prerow;
				row=11-row;
			}
			
			int label=data[prerow-1][precol-1];//选中的是什么棋
			
			JudgeMove jm=new JudgeMove(this,data,isRed);//移动规则判断对象
			switch (label) {
				case 1:
				case 8:
					//是车
					if(!jm.move_che(aixs)){//移动车的规则不合理
						common_op(aixs);//撤销、恢复坐标
						return;
					}
					break;
				case 2:
				case 9:
					//是马
					if(!jm.move_ma(aixs)){//移动马的规则不合理
						common_op(aixs);
						return;
					}
					break;
				case 3:
				case 10:
					//是相
					if(!jm.move_xiang(aixs)){
						common_op(aixs);
						return;
					}
					break;
				case 4:
				case 11:
					//士
					if(!jm.move_shi(aixs)){
						common_op(aixs);
						return;
					}
					break;
				case 5:
				case 12:
					//是将
					if(!jm.move_jiang(aixs)){
						common_op(aixs);
						return;
					}
					break;
				case 6:
				case 13:
					//是炮
					if(!jm.move_pao(aixs)){
						common_op(aixs);
						return;
					}
					break;
				case 7:
				case 14:
					//是兵
					if(!jm.move_bing(aixs)){
						common_op(aixs);
						return;
					}
					break;
				default:
					break;
			}
			
			//可以进行移动
			int[][] datasub=Common.Backup(data);//先备份棋局，下面进行试探性走棋
			datasub[row-1][col-1]=datasub[prerow-1][precol-1];
			datasub[prerow-1][precol-1]=0;
			//判断试探性走棋是否到达游戏结束状态
			if(new HasFinished(datasub).isFinished(isRed)){//游戏结束
				data=datasub;//将当前棋局置为试探性走棋后的棋局
				tip=false;
				isRedo=true;
				repaint();
				internet.writeMessage("Congratulate,you win.");
				//写入悔棋列表中
				rd.add_one_step(Common.Backup(datasub));
				SimpleDateFormat sf=new SimpleDateFormat("YYYYMMDD_HHmmss");
				//写入自动播放的录像中
				Replay.WriteFile(rd.getMove_down(), "file/"+sf.format(new Date())+".chess");
				state=4;//将游戏状态置为结束
				return;
			}
			
			if(!HasFinished.jiangTip(datasub, isRed)){//当前被将军了，不能送将
				if(!isRed)
					prerow=11-prerow;//恢复，解决被将军后移动其他子问题
				isSelected=true;
				return;
			}//当前步不能走

			//到此，表明移动的步符合走棋规则、而且没有送将、以及游戏没有结束
			internet.writeMessage(prerow+" "+precol+" "+row+" "+col);//向服务器写消息
			isSelected=false;//被选中为false，这样下阶段是选择棋子，而不是移动棋子
			isRedo=false;//是走棋，而不是悔棋到达的新棋局状态
		}else{//选子阶段，这是isSelected为false的情况
			precol=col;
			prerow=row;
			if(!isRed){//置换视角
				prerow=11-prerow;
				row=11-row;
			}
			if(precol==0 || prerow==0)//没有选中棋盘
				return;

			if(data[prerow-1][precol-1]==0){//没有选中棋子
				return;
			}else if((isRed && data[prerow-1][precol-1]>=8) || 
					(!isRed && data[prerow-1][precol-1]<=7)){//选择对方的棋子了
				return;
			}
			//到此合理选择了棋子
			tip=true;//开启提示功能
			isSelected=true;//设置isSelected为真，下一阶段是移动棋子而不是选择棋子
			//恢复视角
			if(!isRed){
				prerow=11-prerow;
				row=11-row;
			}
			repaint();
		}
	}

	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e) {}

	//下面是set和get方法
	public boolean isSelected() {
		return isSelected;
	}

	public void setSelected(boolean isSelected) {
		this.isSelected = isSelected;
	}

	public boolean isYourTurn() {
		return yourTurn;
	}

	public void setYourTurn(boolean yourTurn) {
		this.yourTurn = yourTurn;
	}

	public int[][] getData() {
		return data;
	}

	public void setData(int[][] data) {
		this.data = data;
	}

	public Internet getInternet() {
		return internet;
	}

	public Redo getRd() {
		return rd;
	}

	public void setRedo(boolean isRedo) {
		this.isRedo = isRedo;
	}

	public Commication getCc() {
		return cc;
	}

	/**
	 * 与数据服务器和消息服务器进行交互
	 */
	@Override
	public void run() {
		// TODO Auto-generated method stub
		while(true){
			String message_array=internet.readMessage();
			state=3;//设置游戏状态为游戏中
			if(message_array.indexOf(",")>0){//这是接收的是消息
				//结束消息
				JOptionPane.showMessageDialog(null, message_array);
				continue;
			}
			//下面接收的是数据
		    int[][]	datasub=Common.String_to_Array(message_array);
		    int[] aixs=Common.FindDiff(data, datasub,isRed);
		    //获取相对的四个坐标（因为红和视角问题，对于行的设置不同，这里用相对）
		    prerow=aixs[0];
		    if(!isRed) prerow=11-prerow;
		    precol=aixs[1];
		    row=aixs[2];
		    if(!isRed)
		    	row=11-row;
		    col=aixs[3];
		    
		    data=datasub;//将棋局状态置为从服务器中获取的最新的棋局状态数据
		    rd.add_one_step(Common.Backup(data));//将该棋局加入的悔棋列表中
			tip=false;
			repaint();
			yourTurn=!yourTurn;//回合切换
		}
	}
	
	/**
	 * 这是提取的公共操作，用于在不合理的移动棋子时候能恢复到移动前的坐标状态
	 * @param aixs是两个位置的坐标数组
	 */
	private void common_op(int[] aixs) {
		if(isRed){
			prerow=row=aixs[0];
		}else{
			prerow=row=11-aixs[0];
		}
		precol=col=aixs[1];
		repaint();
	}
}
