package tests;
import com.ximpleware.*;
public class test1 {

	public static void main(String[] sv) throws Exception{
		
		String s = "<a><a><b><c><d><e/><e>"
			+"good <bad/> bad <bad/> new <bad/> used"
			+"</e></d></c></b></a>"
			+"<a><b><c><d><e/><e>"
			+"good <bad/> bad <bad/> new <bad/> used"
			+"</e></d></c></b></a>"
			+"</a>";
		VTDGen vg = new VTDGen();
		vg.setDoc(s.getBytes());
		vg.parse(false);
		VTDNav vn = vg.getNav();
		//AutoPilot ap = new AutoPilot(vn);
		vn.toElement(VTDNav.LC);
		System.out.println(" element name "+vn.toString(vn.getCurrentIndex()));
		System.out.println(" element depth "+vn.getCurrentDepth());
		vn.toElement(VTDNav.LC);
		System.out.println(" element name "+vn.toString(vn.getCurrentIndex()));
		System.out.println(" element depth "+vn.getCurrentDepth());
		vn.toElement(VTDNav.LC);
		System.out.println(" element name "+vn.toString(vn.getCurrentIndex()));
		System.out.println(" element depth "+vn.getCurrentDepth());
		vn.toElement(VTDNav.LC);
		System.out.println(" element name "+vn.toString(vn.getCurrentIndex()));
		System.out.println(" element depth "+vn.getCurrentDepth());
		vn.toElement(VTDNav.LC);
		System.out.println(" element name "+vn.toString(vn.getCurrentIndex()));
		System.out.println(" element depth "+vn.getCurrentDepth());
		TextIter ti = new TextIter();
		ti.touch(vn);
		ti.selectText();
		System.out.println(" element name "+vn.toString(vn.getCurrentIndex()));
		System.out.println(" element depth "+vn.getCurrentDepth());
		int i=-1;
		while((i=ti.getNext())!=-1){
			System.out.println(" text node ==> "+vn.toString(i));
		}
		System.out.println("***********\n************");
		if (vg.parseFile("C:\\Users\\Jimmy Zhang\\workspace\\ximple-dev\\DOMTest\\test2.xml", true)){
			vn = vg.getNav();
			AutoPilot ap = new AutoPilot(vn);
			ap.selectXPath("//text()");
			System.out.println("xpath==> "+ ap.getExprString());
			i=-1;
			while((i=ap.evalXPath())!=-1){
				System.out.println(" element name ==>" + vn.toString(i));
			}
		}
		
		
	}
}
