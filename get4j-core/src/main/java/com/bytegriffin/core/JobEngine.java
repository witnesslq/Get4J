package com.bytegriffin.core;

public interface JobEngine
{

  void pause();

  void continues();

  void destory(String jobid);

}
