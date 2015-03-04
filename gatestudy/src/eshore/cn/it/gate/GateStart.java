package eshore.cn.it.gate;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import gate.AnnotationSet;
import gate.Corpus;
import gate.CorpusController;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.annotation.AnnotationImpl;
import gate.creole.ANNIEConstants;
import gate.creole.ConditionalSerialAnalyserController;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;
import gate.util.Out;
import gate.util.persistence.PersistenceManager;

/**
 * class<code>GateStart</code>此类演示了如何利用GATE实现批量信息抽取
 *
 * @author   clebeg
 * @version	 0.0.1
 * @see      java.lang.Class
 * @since    JDK1.8
 * */
public class GateStart {
	private static Logger logger = 
			Logger.getLogger(GateStart.class.getName());
	
	//指定GATE安装目录
	private String gateHome = "F:\\java\\GATE_Developer_8.0";
	//指定注册的plugin名字
	private String pluginName = "Lang_Chinese";
	//指定使用的资源类名
	private String resourceClassName = "gate.corpora.CorpusImpl";
	//指定数据保存的根目录，未实现自动创建，必须手动创建
	private String rootDataHome = "data";
	//指定数据编码方式
	private String fileEncoding = "UTF-8";
	private String corpusDirectory = "corpus";
	
	//定义基于语料库的GATE控制者
	private CorpusController controller;
	private Corpus corpus;
	
	
	public static void main(String[] args) throws Exception {
		GateStart gs = new GateStart();
		gs.gateExecute();
	}
	
	/**
	 * method<code>gateExecute</code>
	 * 此方法可以执行一个完整的GATE信息抽取流程，最终把词性标注的结果保存
	 * */
	public void gateExecute() throws Exception {
		setGateHome();		//设置GATE安装目录
		gateInit();			//初始化GATE，加载必要的插件
		initController();	//初始化Controller
		
		corpus = addFileNameCorpus();	//加入语料信息
		
		controller.setCorpus(corpus);	//提交语料信息
		controller.execute();			//执行GATE流程
		
		persistDocuments();				//持久化执行结果
		
	}
	
	/**
	 * method<code>setGateHome</code>
	 * 此方法可以设置GATE安装目录环境变量
	 * */
	private void setGateHome() {
		System.setProperty("gate.home", this.gateHome);
		logger.info("The GATE_HOME directory is:" + (System.getProperties().getProperty("gate.home")));
	}
	
	/**
	 * method<code>gateInit</code>
	 * 初始化gate，并且加载必要的组件
	 * */
	private void gateInit() throws GateException, MalformedURLException {
		logger.info("...GATE initialise now ");
		Gate.init();
		
		File gateHome = Gate.getGateHome();
		File pluginsHome = new File(gateHome, "plugins");
		
		//这里需要设定pluginName参数，指定plugin的名字
		Gate.getCreoleRegister().registerDirectories(new File(pluginsHome, this.pluginName).toURI().toURL());
		
		logger.info("...GATE initialised ");
	}
	
	/**
	 * method<code>initController</code>
	 * 初始化控制器,可以初始化成不同的控制器
	 * */
	private void initController() throws GateException, IOException {
		logger.info("Initialising controller...");
		//chnController = (ConditionalSerialAnalyserController) PersistenceManager.loadObjectFromFile(new File(new File(Gate.getPluginsHome(),"Lang_Chinese"), "resources/chinese.gapp"));
		controller = (ConditionalSerialAnalyserController)
				PersistenceManager.loadObjectFromFile(new File(new File(
				Gate.getPluginsHome(), ANNIEConstants.PLUGIN_DIR),
				ANNIEConstants.DEFAULT_FILE));
		logger.info("...controller loaded");
	} 
	
	
	/**
	 * method<code>addFileNameCorpus</code>
	 * 加入文档到语料库，改变这个方法可以批量加入语料
	 * */
	private Corpus addFileNameCorpus() throws ResourceInstantiationException, MalformedURLException {
		File cd = new File(corpusDirectory);
		File[] childs = null;
		if (cd.isDirectory()) 
			childs = cd.listFiles();
		
		//需要指定资源加载语料库的资源类
		Corpus corpus = (Corpus) Factory.createResource(this.resourceClassName);
		
		for (File file : childs) {
			if (file.isFile()) {
				StringBuffer corpusDir = new StringBuffer("");
				corpusDir.append("file:/").append(file.getAbsolutePath());
				URL u = new URL(corpusDir.toString());
				FeatureMap params = Factory.newFeatureMap();
				params.put("sourceUrl", u);
				params.put("preserveOriginalContent", new Boolean(true));
				params.put("collectRepositioningInfo", new Boolean(true));
				params.put("encoding", fileEncoding);//以UTF-8编码读取信息 否则会出现乱码
				Out.prln("Creating doc for " + u);
				Document docs = (Document) Factory.createResource("gate.corpora.DocumentImpl", params);
				corpus.add(docs);
			}
		}
		return corpus;
	}


	
	/**
	 * method<code>persistDocuments</code>
	 * 持久化经过GATE处理之后的文件为xml,并且保存到指定目录中
	 * */
	private void persistDocuments() throws IOException {
		Iterator<Document> it = corpus.iterator();
		while(it.hasNext()) {
			Document doc = it.next();
			FileWriter writer = new FileWriter(new File(this.rootDataHome, doc.getName() + ".xml"));
			writer.write(doc.toXml());
			writer.close();
		}
	}

	
	

    /**
     * 此方法为测试方法
     * */
//	private void doSometings(Document docs) throws ResourceInstantiationException {
//		AnnotationSet annSet = docs.getAnnotations();
//		
//		String type = "Date";
//		
//		AnnotationSet persSet = annSet.get(type);
//		List persList = new ArrayList(persSet);
//		Collections.sort(persList , new gate.util.OffsetComparator());
//		Iterator persIter = persList.iterator();
//		while(persIter.hasNext()) {
//			AnnotationImpl impl = (AnnotationImpl)persIter.next();
//			
//		}
//	}

	
	
	
}
