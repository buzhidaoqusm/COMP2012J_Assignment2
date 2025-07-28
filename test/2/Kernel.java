import java.lang.Thread;
import java.io.*;
import java.util.*;


public class Kernel extends Thread
{
  private static int virtPageNum = 63;

  private String output = null;
  private static final String lineSeparator = 
    System.getProperty("line.separator");
  private String command_file;
  private String config_file;
  private Vector<Page> memVector = new Vector<>();
  // private Vector<Page> physicalMem = new Vector<>();

  private HashMap<Integer, Page> physicalMem = new HashMap<Integer, Page>();

  private final Vector<Instruction> instructVector = new Vector<Instruction>();
  private String status;
  private static String replacementAlgo = "FIFO";
  private boolean doStdoutLog = false;
  private boolean doFileLog = false;
  private int physicalMemSize;
  public int runs;
  public int runcycles;
  public long block = (int) Math.pow(2,12);
  public static byte addressradix = 10;
  public static FileOutputStream log;

  public void init( String commands , String config ) throws FileNotFoundException {
    File f;
    command_file = commands;
    config_file = config;
    String line;
    String tmp = null;
    String command = "";
    byte R = 0;
    byte M = 0;
    int i = 0;
    int j = 0;
    int id = 0;
    int physical = 0;
    int physical_count = 0;
    int inMemTime = 0;
    int lastTouchTime = 0;
    int map_count = 0;
    double power = 14;
    long high = 0;
    long low = 0;
    long addr = 0;
    long address_limit = (block * virtPageNum+1)-1;
    physicalMemSize = 32;

    if ( config != null )
    {
      f = new File ( config );
      try 
      {
        BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
        while ((line = in.readLine()) != null) 
        {
          if (line.startsWith("numpages")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              virtPageNum = Common.s2i(st.nextToken()) - 1;
              if ( virtPageNum < 2 || virtPageNum > 63 )
              {
                System.out.println("MemoryManagement: numpages out of bounds.");
                System.exit(-1);
              }
              address_limit = (block * virtPageNum+1)-1;
            }
          }
        }
        in.close();
      } catch (IOException e) { /* Handle exceptions */
        e.printStackTrace();
      }
      for (i = 0; i <= virtPageNum; i++) 
      {
        high = (block * (i + 1))-1;
        low = block * i;
        memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
      }
      try 
      {
        BufferedReader in = new BufferedReader(new InputStreamReader((new FileInputStream(f))));
        while ((line = in.readLine()) != null)
        {
          if (line.startsWith("memset")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            st.nextToken();
            while (st.hasMoreTokens()) 
            { 
              id = Common.s2i(st.nextToken());
              tmp = st.nextToken();
              if (tmp.startsWith("x")) 
              {
                physical = -1;
              } 
              else 
              {
                physical = Common.s2i(tmp);
              }
              if ((0 > id || id > virtPageNum) || (-1 > physical || physical >= physicalMemSize))
              {
                System.out.println("MemoryManagement: Invalid page value in " + config);
                System.exit(-1);
              }
              R = Common.s2b(st.nextToken());
              if (R < 0 || R > 1)
              {
                System.out.println("MemoryManagement: Invalid R value in " + config);
                System.exit(-1);
              }
              M = Common.s2b(st.nextToken());
              if (M < 0 || M > 1)
              {
                 System.out.println("MemoryManagement: Invalid M value in " + config);
                 System.exit(-1);
              }
              inMemTime = Common.s2i(st.nextToken());
              if (inMemTime < 0)
              {
                System.out.println("MemoryManagement: Invalid inMemTime in " + config);
                System.exit(-1);
              }
              lastTouchTime = Common.s2i(st.nextToken());
              if (lastTouchTime < 0)
              {
                System.out.println("MemoryManagement: Invalid lastTouchTime in " + config);
                System.exit(-1);
              }
              Page page = memVector.elementAt(id);
              page.physical = physical;
              page.R = R;
              page.M = M;
              page.inMemTime = inMemTime;
              page.lastTouchTime = lastTouchTime;
              page.reference_bit = true;
              // physicalMem.add(physical, page);
              physicalMem.put(physical, page);
            }
          }
          if (line.startsWith("enable_logging")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              if ( st.nextToken().startsWith( "true" ) )
              {
                doStdoutLog = true;
              }              
            }
          }
          if (line.startsWith("log_file")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
            }
            if ( tmp.startsWith( "log_file" ) )
            {
              doFileLog = false;
              output = "tracefile";
            }              
            else
            {
              doFileLog = true;
              doStdoutLog = false;
              output = tmp;
            }
          }
          if (line.startsWith("pagesize")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              if ( tmp.startsWith( "power" ) )
              {
                power = Integer.parseInt(st.nextToken());
                block = (int) Math.pow(2,power);
              }
              else
              {
                block = Long.parseLong(tmp,10);             
              }
              address_limit = (block * (virtPageNum+1))-1;
            }
            if ( block < 64 || block > Math.pow(2,26))
            {
              System.out.println("MemoryManagement: pagesize is out of bounds");
              System.exit(-1);
            }
            for (i = 0; i <= virtPageNum; i++) 
            {
              Page page = memVector.elementAt(i);
              page.high = (block * (i + 1))-1;
              page.low = block * i;
            }
          }
          if (line.startsWith("addressradix")) 
          { 
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens()) 
            {
              tmp = st.nextToken();
              tmp = st.nextToken();
              addressradix = Byte.parseByte(tmp);
              if ( addressradix < 0 || addressradix > 20 )
              {
                System.out.println("MemoryManagement: addressradix out of bounds.");
                System.exit(-1);
              }
            }
          }
          if (line.startsWith("replacementAlgorithm"))
          {
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens())
            {
              tmp = st.nextToken();
              tmp = st.nextToken();

              replacementAlgo = tmp;
            }
          }
          if (line.startsWith("physicalMemSize"))
          {
            StringTokenizer st = new StringTokenizer(line);
            while (st.hasMoreTokens())
            {
              tmp = st.nextToken();
              physicalMemSize = Common.s2i(st.nextToken());
            }
          }
        }
        in.close();
      } catch (IOException e) {
          e.printStackTrace();
      }
    }
    f = new File ( commands );
    try 
    {
      BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(f)));
      while ((line = in.readLine()) != null) 
      {
        if (line.startsWith("READ") || line.startsWith("WRITE")) 
        {
          if (line.startsWith("READ")) 
          {
            command = "READ";
          }
          if (line.startsWith("WRITE")) 
          {
            command = "WRITE";
          }
          StringTokenizer st = new StringTokenizer(line);
          tmp = st.nextToken();
          tmp = st.nextToken();
          if (tmp.startsWith("random")) 
          {
            instructVector.addElement(new Instruction(command,Common.randomLong( address_limit )));
          } 
          else 
          { 
            if ( tmp.startsWith( "bin" ) )
            {
              addr = Long.parseLong(st.nextToken(),2);             
            }
            else if ( tmp.startsWith( "oct" ) )
            {
              addr = Long.parseLong(st.nextToken(),8);
            }
            else if ( tmp.startsWith( "hex" ) )
            {
              addr = Long.parseLong(st.nextToken(),16);
            }
            else
            {
              addr = Long.parseLong(tmp);
            }
            if (0 > addr || addr > address_limit)
            {
              System.out.println("MemoryManagement: " + addr + ", Address out of range in " + commands);
              System.exit(-1);
            }
            instructVector.addElement(new Instruction(command,addr));
          }
        }
      }
      in.close();
    } catch (IOException e) { /* Handle exceptions */ }
    runcycles = instructVector.size();
    if ( runcycles < 1 )
    {
      System.out.println("MemoryManagement: no instructions present for execution.");
      System.exit(-1);
    }
    if ( doFileLog )
    {
      File trace = new File(output);
      trace.delete();
      log = new FileOutputStream(output);
    }
    runs = 0;
    for (i = 0; i <= virtPageNum; i++)
    {
      Page page = memVector.elementAt(i);
      if ( page.physical != -1 )
      {
        map_count++;
      }
      for (j = 0; j <= virtPageNum; j++)
      {
        Page tmp_page = memVector.elementAt(j);
        if (tmp_page.physical == page.physical && page.physical >= 0)
        {
          physical_count++;
        }
      }
      if (physical_count > 1)
      {
        System.out.println("MemoryManagement: Duplicate physical page's in " + config);
        System.exit(-1);
      }
      physical_count = 0;
    }
    if ( map_count < physicalMemSize )
    {
      for (i = 0; i <= virtPageNum; i++)
      {
        Page page = memVector.elementAt(i);
        if ( page.physical == -1 && map_count < physicalMemSize )
        {
          page.physical = i;
          // physicalMem.add(i, page);
          physicalMem.put(i, page);
          map_count++;
        }
      }
    }
    for (i = 0; i < instructVector.size(); i++) 
    {
      high = address_limit;
      Instruction instruct = instructVector.elementAt( i );
      if ( instruct.addr < 0 || instruct.addr > high )
      {
        System.out.println("MemoryManagement: Instruction (" + instruct.inst + " " + instruct.addr + ") out of bounds.");
        System.exit(-1);
      }
    }
  } 


  public void getPage(int pageNum) 
  {
    Page page = memVector.elementAt( pageNum );
//    controlPanel.paintPage( page );
  }

  private void printLogFile(String message)
  {

    try {
      log = new FileOutputStream(output, true);
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(log));
      out.write(message);
      out.newLine();

      out.close();
    }
    catch (IOException e) 
    {
      e.printStackTrace();
    }
  }

  public void run()
  {
    step();
    while (runs != runcycles) 
    {
      try 
      {
        Thread.sleep(2000);
      } 
      catch(InterruptedException e) 
      {  
        e.printStackTrace();
      }
      step();
    }  
  }

  public void step()
  {
    int i = 0;

    Instruction instruct = instructVector.elementAt( runs );
    if ( instruct.inst.startsWith( "READ" ) ) 
    {
      Page page = memVector.elementAt( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
      if ( page.physical == -1 )
      {
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum , Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ), output, physicalMem, replacementAlgo );
      } 
      else 
      {
        page.R = 1;
        page.lastTouchTime = 0;   
        if ( doFileLog )
        {
          printLogFile( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "READ " + Long.toString( instruct.addr , addressradix ) + " ... okay" );
        }
        page.reference_bit = true;
      }
    }
    if ( instruct.inst.startsWith( "WRITE" ) ) 
    {
      Page page = memVector.elementAt( Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ) );
      if ( page.physical == -1 )
      {
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        if ( doStdoutLog )
        {
           System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... page fault" );
        }
        PageFault.replacePage( memVector , virtPageNum , Virtual2Physical.pageNum( instruct.addr , virtPageNum , block ), output, physicalMem, replacementAlgo );
      } 
      else 
      {
        page.M = 1;
        page.lastTouchTime = 0;
        if ( doFileLog )
        {
          printLogFile( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
        if ( doStdoutLog )
        {
          System.out.println( "WRITE " + Long.toString(instruct.addr , addressradix) + " ... okay" );
        }
        page.reference_bit = true;
      }
    }
    for ( i = 0; i <= virtPageNum; i++ )
    {
      Page page = memVector.elementAt( i );
      // Reseting R in the next round
      if ( page.R == 1 && page.lastTouchTime == 10 ) 
      {
        page.R = 0;
      }
      if ( page.physical != -1 )
      {
        page.inMemTime = page.inMemTime + 10;
        page.lastTouchTime = page.lastTouchTime + 10;
      }
    }
    runs++;
  }

  public void reset() throws FileNotFoundException {
    memVector.removeAllElements();
    instructVector.removeAllElements();
    init( command_file , config_file );
  }
}
