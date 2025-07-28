/* It is in this file, specifically the replacePage function that will
   be called by MemoryManagement when there is a page fault.  
*/

  // This PageFault file is an example of the FIFO Page Replacement 
  // Algorithm.

import java.io.*;
import java.util.*;


public class PageFault {
  private static int hand = 0; // The hand of the clock policy, pointing to the next page to be checked
  /**
   * The page replacement algorithm for the memory management simulator.
   * This method gets called whenever a page needs to be replaced.
   * <p>
   * The page replacement algorithm included with the simulator is 
   * FIFO (first-in first-out).  A while or for loop should be used 
   * to search through the current memory contents for a candidate
   * replacement page.  In the case of FIFO the while loop is used 
   * to find the proper page while making sure that virtPageNum is 
   * not exceeded.
   * <pre>
   *   Page page = ( Page ) mem.elementAt( oldestPage )
   * </pre>
   * This line brings the contents of the Page at oldestPage (a 
   * specified integer) from the mem vector into the page object.  
   * Next recall the contents of the target page, replacePageNum.  
   * Set the physical memory address of the page to be added equal 
   * to the page to be removed.
   * One must also remember to reset the values of
   * the page which has just been removed from memory.
   *
   * @param mem is the vector which contains the contents of the pages 
   *   in memory being simulated.  mem should be searched to find the 
   *   proper page to remove, and modified to reflect any changes.  
   * @param virtPageNum is the number of virtual pages in the 
   *   simulator (set in Kernel.java).  
   * @param replacePageNum is the requested page which caused the 
   *   page fault.
   * @param output is the name of the log file.
   * @param physicalMem is the data structure that have the mapping of physical memory to pages.
   */
  public static void replacePage ( Vector mem , int virtPageNum , int replacePageNum, String output, HashMap physicalMem, String replacementAlgo ) {
    switch (replacementAlgo) {
      case "FIFO":
        //Calling FIFO replacement method
        FIFO(mem, virtPageNum, replacePageNum, output);
        break;
      case "LRU":
        //Call LRU method
        LRU(mem, virtPageNum, replacePageNum, output);
        break;
      case "Clock_policy":
        //Call clock_policy replacement method
        Clock_policy(mem, replacePageNum, physicalMem, output);
        break;
      default:
        System.out.println("Wrong policy name.");
        break;
    }
  }

  public static void FIFO(Vector mem, int virtPageNum, int replacePageNum, String output){
    int count = 0;
    int oldestPage = -1;
    int oldestTime = 0;
    int firstPage = -1;
    boolean mapped = false;

    while ( ! (mapped) || count <= virtPageNum ) {
      Page page = ( Page ) mem.elementAt( count );
      if ( page.physical != -1 ) {
        if (firstPage == -1) {
          firstPage = count;
        }
        if (page.inMemTime > oldestTime) {
          oldestTime = page.inMemTime;
          oldestPage = count;
          mapped = true;
        }
      }
      count++;
      if ( count == virtPageNum ) {
        mapped = true;
      }
    }
    if (oldestPage == -1) {
      oldestPage = firstPage;
    }
    Page page = ( Page ) mem.elementAt( oldestPage );
    Page nextpage = ( Page ) mem.elementAt( replacePageNum );

    printReplacement(page, nextpage, output);
    nextpage.physical = page.physical;

    page.inMemTime = 0;
    page.lastTouchTime = 0;
    page.R = 0;
    page.M = 0;
    page.physical = -1;
  }

  public static void LRU(Vector mem, int virtPageNum, int replacePageNum, String output) {
    int count = 0;
    int leastRecentlyUsedPage = -1; // The id of page to be replaced
    int leastRecentTime = 0; // The last touch time of the page to be replaced
    int firstPage = -1; // The id of the first page in memory
    boolean mapped = false; // Whether the least recently used page has been found

    // Find the least recently used page in memory
    while (!mapped || count <= virtPageNum) {
      Page page = (Page) mem.elementAt(count);
      if (page.physical != -1) {
        // If the first page in memory has not been found, set it to the current page
        if (firstPage == -1) {
          firstPage = count;
        }
        // If two pages have the same last touch time, the one with smaller id will remain in memory, just like FIFO
        if (page.lastTouchTime > leastRecentTime) {
          leastRecentTime = page.lastTouchTime;
          leastRecentlyUsedPage = count;
          mapped = true;
        }
      }
      count++;
      // If all pages in memory have been checked, stop the loop
      if (count == virtPageNum) {
        mapped = true;
      }
    }
    // If no page has been found, replace the first page in memory
    if (leastRecentlyUsedPage == -1) {
      leastRecentlyUsedPage = firstPage;
    }
    Page page = (Page) mem.elementAt(leastRecentlyUsedPage);
    Page nextpage = (Page) mem.elementAt(replacePageNum);

    // Log the replacement
    printReplacement(page, nextpage, output);
    nextpage.physical = page.physical;

    // Reset the old page's attributes
    page.inMemTime = 0;
    page.lastTouchTime = 0;
    page.R = 0;
    page.M = 0;
    page.physical = -1;
  }

  public static void Clock_policy(Vector mem, int replacePageNum, HashMap<Integer, Page> physicalMem, String output) {
    boolean pageReplaced = false; // Whether the page has been replaced
    List<Page> pageList = physicalMem.values().stream().toList(); // The list of pages currently in physical memory
    while (!pageReplaced) {
      Page page = pageList.get(hand);
      if (page.reference_bit) {
      // If the page's reference bit is true, give the page the second chance.
      // Clear the reference bit and move to the next page
      page.reference_bit = false;
      } else {
      Page nextpage = (Page) mem.elementAt(replacePageNum); // The page to be replaced
      printReplacement(page, nextpage, output); // Log the replacement
      nextpage.physical = page.physical; // Replace the page
      nextpage.reference_bit = true; // Set the reference bit of the new page to true
      physicalMem.replace(page.physical, nextpage); // Update the physical memory mapping
      // Reset the old page's attributes
      page.inMemTime = 0;
      page.lastTouchTime = 0;
      page.R = 0;
      page.M = 0;
      page.physical = -1;
      pageReplaced = true;
      }
      // Move the clock hand to the next page
      hand = (hand + 1) % physicalMem.size();
    }
  }
// Another way to implement the Clock policy, but I'm not sure if the page should be sorted by physical address, so I commented it out
//  public static void Clock_policy(Vector mem, int replacePageNum, String output) {
//    int hand = 0;
//    boolean pageReplaced = false; // Whether the page has been replaced
//
//    // Create a list of pages currently in physical memory
//    Vector<Page> pages = new Vector<>();
//    for (int i = 0; i < mem.size(); i++) {
//      Page page = (Page) mem.elementAt(i);
//      if (page.physical != -1) {
//        pages.add(page);
//      }
//    }
//    // Sort pages by their physical memory address
//    pages.sort(Comparator.comparingInt(o -> o.physical));
//
//    // Find the page with the shortest in-memory time, if the same, find the one with the highest physical address
//    int shortestTime = Integer.MAX_VALUE;
//    for (int i = 0; i < pages.size(); i++) {
//      Page page = pages.elementAt(i);
//      if (page.physical != -1) {
//        if (page.inMemTime <= shortestTime) {
//          shortestTime = page.inMemTime;
//          hand = i;
//        }
//      }
//    }
//    // The clock policy should start from the page after the one with the shortest in-memory time
//    hand = (hand + 1) % pages.size();
//
//    // Iterate through the pages using the clock hand
//    while (!pageReplaced) {
//      Page page = pages.elementAt(hand);
//      if (page.physical != -1) {
//        if (page.reference_bit) {
//          // If the page's reference bit is true, give the page the second chance.
//          // Clear the reference bit and move to the next page
//          page.reference_bit = false;
//        } else {
//          // Replace the page
//          Page nextpage = (Page) mem.elementAt(replacePageNum);
//
//          // Log the replacement
//          printReplacement(page, nextpage, output);
//          nextpage.physical = page.physical;
//          nextpage.reference_bit = true;
//
//          // Reset the old page's attributes
//          page.inMemTime = 0;
//          page.lastTouchTime = 0;
//          page.R = 0;
//          page.M = 0;
//          page.physical = -1;
//
//          pageReplaced = true;
//        }
//      }
//      // Move the clock hand to the next page
//      hand = (hand + 1) % pages.size();
//    }
//  }

  private static void printReplacement(Page oldPage, Page newPage, String output){
    try {
      FileOutputStream log = new FileOutputStream(output, true);
      BufferedWriter out = new BufferedWriter(new OutputStreamWriter(log));
      out.write("Page:"+ oldPage.id +" replaced by page:"+ newPage.id);
      out.newLine();

      out.close();
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
}
