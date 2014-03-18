package com.wst.sweetfm.topology;

import com.wst.sweetfm.bolt.MTSA_DRPC_DistanceCalculationBatchBolt;
import com.wst.sweetfm.bolt.MTSA_DPRC_TopicReduceBolt;
import com.wst.sweetfm.bolt.MTSA_DRPC_DistanceReduceBatchBolt;
import com.wst.sweetfm.bolt.MTSA_DRPC_SeriesAnalyzeBatchBolt;

import com.wst.sweetfm.bolt.MTSA_DPRC_SplitBolt;

import backtype.storm.Config;
import backtype.storm.LocalCluster;
import backtype.storm.LocalDRPC;
import backtype.storm.StormSubmitter;
import backtype.storm.drpc.LinearDRPCTopologyBuilder;

@SuppressWarnings("deprecation")
public class DRPC_MTSATopology {
	public static void main(String[] args) throws Exception{
		LinearDRPCTopologyBuilder builder = new LinearDRPCTopologyBuilder("sweetfm");
		builder.addBolt(new MTSA_DPRC_SplitBolt(),1);
	    builder.addBolt(new MTSA_DRPC_SeriesAnalyzeBatchBolt(), 5).shuffleGrouping();
	    builder.addBolt(new MTSA_DPRC_TopicReduceBolt(), 1).shuffleGrouping();
	    builder.addBolt(new MTSA_DRPC_DistanceCalculationBatchBolt(), 10).shuffleGrouping();
	    builder.addBolt(new MTSA_DRPC_DistanceReduceBatchBolt(), 1).shuffleGrouping();

	    Config conf = new Config();
	    //conf.setMessageTimeoutSecs(60);

	    if (args == null || args.length == 0) {
	      LocalDRPC drpc = new LocalDRPC();
	      LocalCluster cluster = new LocalCluster();

	      cluster.submitTopology("mtsa-drpc-demo", conf, builder.createLocalTopology(drpc));
          
	      String subSeq = "1>2>3>4>5>6>7>8>9>10>11>12>13>14>15>16>17>18>19>20>21>22>23>24>25";
	      long startTime = System.currentTimeMillis();
	      System.err.println("Result for \"" + subSeq + "\": " + drpc.execute("sweetfm", subSeq));
	      long endTime = System.currentTimeMillis();
	      
	      System.err.println("Consumed:"+(endTime-startTime));
	      
	      cluster.shutdown();
	      drpc.shutdown();
	    }
	    else {
	      conf.setNumWorkers(3);
	      StormSubmitter.submitTopology(args[0], conf, builder.createRemoteTopology());
	    }
	}
}