package storm.starter;

import java.util.Map;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.OutputCollector;
import backtype.storm.task.ShellBolt;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.IRichBolt;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.TopologyBuilder;
import backtype.storm.topology.base.BaseRichBolt;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Tuple;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

public class SimpleRTopology {
	static class SimpleSpout extends BaseRichSpout{
		private SpoutOutputCollector collector;
		@Override
		public void open(Map conf, TopologyContext context,
				SpoutOutputCollector collector) {
			// TODO Auto-generated method stub
			this.collector = collector;
		}

		@Override
		public void nextTuple() {
			// TODO Auto-generated method stub
			collector.emit(new Values("Hello"));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			// TODO Auto-generated method stub
			declarer.declare(new Fields("word"));
		}
	}
	static class StartBolt extends BaseRichBolt{
		private OutputCollector collector;
		private boolean flag = false;
		@Override
		public void prepare(Map stormConf, TopologyContext context,
				OutputCollector collector) {
			// TODO Auto-generated method stub
			this.collector = collector;
		}

		@Override
		public void execute(Tuple input) {
			// TODO Auto-generated method stub
			Utils.sleep(1000);
			System.err.println("StartBolt:"+input.getString(0));
			collector.emit(new Values(input.getString(0)+"!!!"));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			// TODO Auto-generated method stub
			declarer.declare(new Fields("word"));
		}
	}
	
	static class RBolt extends ShellBolt implements IRichBolt{

		public RBolt(){
			super("Rscript", "simple.R");
			System.err.println("I am in RBolt...");
		}
		
		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			// TODO Auto-generated method stub
			declarer.declare(new Fields("word"));
		}

		@Override
		public Map<String, Object> getComponentConfiguration() {
			// TODO Auto-generated method stub
			return null;
		}
	}
	
	static class EndBolt extends BaseRichBolt{
		private OutputCollector collector;
		@Override
		public void prepare(Map stormConf, TopologyContext context,
				OutputCollector collector) {
			// TODO Auto-generated method stub
			this.collector = collector;
		}

		@Override
		public void execute(Tuple input) {
			// TODO Auto-generated method stub
			System.err.println("EndBolt:"+input.getString(0));
		}

		@Override
		public void declareOutputFields(OutputFieldsDeclarer declarer) {
			// TODO Auto-generated method stub
			
		}
	}
	
	public static void main(String[] args){
		TopologyBuilder builder = new TopologyBuilder();
		builder.setSpout("spout1", new SimpleSpout(),1);
		builder.setBolt("start1", new StartBolt(),1).shuffleGrouping("spout1");
		builder.setBolt("rcall1", new RBolt(),1).shuffleGrouping("start1");
		builder.setBolt("end1", new EndBolt(),1).shuffleGrouping("rcall1");
		
		Config conf = new Config();
		conf.setDebug(true);
		
		LocalCluster cluster = new LocalCluster();
		cluster.submitTopology("simple", conf, builder.createTopology());
		
		Utils.sleep(15000);
		cluster.killTopology("simple");
		cluster.shutdown();
	}
}