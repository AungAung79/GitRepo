#!/usr/bin/python
# -*- coding:utf-8 -*-
"""Perform experiment to verify 'longer sequence cannot get better hit ratio'.
With the conclusion, we can set the maximum length of time series.
Dependecies:
  util.
  const.
"""
__author__ = 'Jason Wong(wwssttt@163.com)'
__version__ = '1.0'

import time
import persist
import predict
import util
import logging
import os
import math
import matplotlib.pyplot as plt
import sys
import const

# reload sys config and set default encoding as utf-8
reload(sys)
sys.setdefaultencoding('utf-8')

# set log's localtion and level
logging.basicConfig(filename=os.path.join(os.getcwd(),
                    '../log/arima_trend_test_log_%s.txt' % const.DATASET_NAME),
                    level=logging.DEBUG,
                    format='%(asctime)s-%(levelname)s:%(message)s')

def getResultOfArimaMethod():
  """Run MTSA with different maximum length of time series
  and then return the average hit ratio, precision, f1, mae and rmse.
  Input:
    None.
  Output:
    result - result of experiments.
  """
  start_time = time.time()
  songDict = persist.readSongFromFile() # read total songs from file.
  allPlaylist = persist.readPlaylistFromFile() # read all playlists from file.
  result = []
  totalRecalls = []
  totalPrecisions = []
  totalF1s = []
  totalMaes = []
  totalRmses = []
  for length in range(5,51,5): # set maximum length between 5 to 50.
    recalls = 0.0
    precisions = 0.0
    f1s = 0.0
    maes = 0.0
    rmses = 0.0
    for scale in range(10):
      playlistDict = allPlaylist[scale] # get the playlist in specific scale
      recDict = predict.getRecDict(playlistDict,songDict,const.ARIMA,scale,
                                     const.TOP_N,length) # get predicted result
      #get the middle result
      recall,precision,f1 = util.getTopNIndex(recDict,playlistDict,const.TOP_N)
      mae,rmse = util.getMAEandRMSE(recDict,playlistDict,songDict,const.TOP_N)
      #add to summary
      recalls += recall
      precisions += precision
      f1s += f1
      maes += mae
      rmses += rmse

    #cal the avg value
    recalls = recalls / 10
    precisions = precisions / 10 
    f1s = f1s / 10
    maes = maes / 10
    rmses = rmses / 10

    #log info to lod file
    print 'Length = %d:%f %f %f %f %f' % (length,recalls,precisions,f1s,maes,rmses)
    logging.info('%d:%f %f %f %f %f' % (length,recalls,precisions,f1s,maes,rmses))
    #add result to list
    totalRecalls.append(recalls)
    totalPrecisions.append(precisions)
    totalF1s.append(f1s)
    totalMaes.append(maes)
    totalRmses.append(rmses)

  result.append(totalRecalls)
  result.append(totalPrecisions)
  result.append(totalF1s)
  result.append(totalMaes)
  result.append(totalRmses)
  end_time = time.time()
  print 'Consumed:%d' % (end_time-start_time)
  return result  

def showResult():
  """Get final result from getResultOfArimaMethod()
  and then plot it using pyplot.
  Input:
    None.
  Output:
    None.
  """
  logging.info('I am in showResult......')
  result = getResultOfArimaMethod() # get the result
  length = range(5,51,5)
  #plt imgs of result
  for index in range(5):
    indexName = util.getIndexName(index) # get index name
    plt.figure(index)
    plt.plot(length,result[index])
    plt.title("%s of MTSA with Different Window Sizes of Session" % indexName)
    plt.xlabel("window size of session")
    plt.ylabel(indexName)
    plt.legend()
    plt.savefig("../img/arima_trend_%s.png" % indexName)
    plt.show()
  logging.info('I am out showResult......')

if __name__ == "__main__":
  showResult()