# Task-dispenser
##远程任务中转发送器
功能:联合Blog系统和remoteMsgTask项目(client)。完成web端的远程任务操作。<br>
浏览器----->发送任务(https)----->blog系统远程任务发送页面----->redis消息---->task-executor---->socket连接---->remoteMsgTask(执行任务)<br>
目前任务类型 ：
  1. Download 下载任务，支持http或磁链下载。下载工具为Aria2
  2. Python,直接执行Python脚本，理论上可以完成大部分对PC端的操作。

实现,spring-boot +  redis + netty(server)

  
