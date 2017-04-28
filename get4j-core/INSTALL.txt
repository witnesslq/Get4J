
                          Get4J

  它是啥?
  -----------

  Get4J是一款基于流程式的爬虫。

  系统需求
  -------------------

  JDK：1.8或以上
  内存：至少2G以上。
  磁盘：至少2G以上。
  操作系统:
    Windows:
       Windows 2000以上.
    Unix based systems (Linux, Solaris and Mac OS X) 或者其他:
       Linux, Solaris, Mac OS X, Ubuntu CentOS 等等.

  安装
  ----------------
  1) 请确保本机已经安装Git，Maven，并且配置好了JDK1.8。

  2) 下载整个项目 git clone https://github.com/bytegriffin/Get4J.git，会得到名为Get4J的文件夹。

  3) 用命令行进入到Get4J/get4j-core目录，并执行 mvn clean install package 命令。

  4) 在Get4J/get4j-core/target目录下会得到一个名为get4j-core-x.y.z-release.tar.gz的文件。
     解压这个文件, 例如: tar zxvf get4j-core-x.y.z-release.tar.gz

  5) 解压“get4j-core-x.y.z-bin.tar.gz”这个文件，会得到一个名为"get4j-core-x.y.z-bin"的文件夹。

  6) 将文件夹"get4j-core-x.y.z-bin"复制到你想安装的目录下，并配置环境变量, 例如:
    Unix-based operating systems
      export PATH=/opt/get4j-core-x.y.z-release/bin:$PATH
    Windows
      set PATH="C:\Program Files\get4j-core-x.y.z-release\bin";%PATH%

  7) 在命令行输入"spider"确认是否已配置成功。

  协议
  ---------

  请参看同目录下的LICENSE文件.

