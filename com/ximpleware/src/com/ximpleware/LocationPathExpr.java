/* 
 * Copyright (C) 2002-2010 XimpleWare, info@ximpleware.com
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package com.ximpleware;
import com.ximpleware.xpath.*;
// if the context node is text(),
// then many axis simply won't work
/**
 * LocationPathExpr implements the location path expression
 * as defined in XPath spec
 */
public class LocationPathExpr extends Expr{

		public static final int ABSOLUTE_PATH =0,
					RELATIVE_PATH =1;
		Step s;
		Step currentStep;
		int pathType;
		int state;
	    //FastIntBuffer fib; // for uniqueness checking
	    intHash ih;
	    
		public static final int START = 0, // initial state
					   END= 1,   // return to begin
				      TERMINAL= 2, // no more next step
				      FORWARD = 3, // 
				      BACKWARD= 4;
								
		public LocationPathExpr(){
			state = START;
			s = null;
			pathType = RELATIVE_PATH;
			currentStep = null;
			//fib = new FastIntBuffer(8);// page size 256 = 2^ 8
			ih = new intHash();
		}
		
		public void setStep(Step st){
			s = st;
		}
		
		public void setPathType(int ptype){
			pathType = ptype;
		}
//		 Improved version for uniqueness checking
		public boolean isUnique(int i){
		    return ih.isUnique(i);
		}
		
		public void reset(VTDNav vn){
			state = START;
			Step temp = s;
			ih.reset();
			currentStep = null;
			while(temp!=null){
				temp.reset(vn);
				temp = temp.nextS;
			}	
		}

		public String toString(){
			String st = "";
			Step ts = s;
			if (pathType == LocationPathExpr.ABSOLUTE_PATH){
				st = st+"/";
			}
			if (ts == null)
				return st;
			else 
				return st + ts;
		}

		public boolean evalBoolean(VTDNav vn){
			boolean a = false;
			vn.push2();
			// record stack size
			int size = vn.contextStack2.size;
		        try{	
				a = (evalNodeSet(vn) != -1);
			
			}catch (Exception e){
			}
			//rewind stack
			vn.contextStack2.size = size;
			reset(vn);
			vn.pop2();
			return a;
		}

		public double evalNumber(VTDNav vn){
			int a = getStringIndex(vn);
			try{
				if (a!=-1) return vn.parseDouble(a);
			}catch (NavException e){
			}
			return Double.NaN;
		}
	
		public String evalString(VTDNav vn){ 	
			int a = getStringIndex(vn);
        try {
            if (a != -1)
                return vn.toString(a);
        } catch (NavException e) {
        }
        return "";	
		}

		public boolean  isNodeSet(){
			return true;
		}

		public boolean  isNumerical(){
			return false;
		}
		
		
	private final int process_child(VTDNav vn)throws XPathEvalException,NavException{
		    int result;
		    boolean b = false, b1 = false;
		    Predicate t= null;
		    
		    switch(state){
		    	case START:
		    	    if (currentStep.nt.testType < NodeTest.TEXT){
		    	        // first search for any predicate that 
		    	        // requires contextSize
		    	        // if so, compute its context size
		    	        // if size > 0
		    	        // set context
		    	        // if size ==0 
		    	        // immediately set the state to backward or end
		    	        t = currentStep.p;
		    	        while(t!=null){
		    	            if (t.requireContextSize()){
		    	                int i = computeContextSize(t,vn);
		    	                if (i==0){
		    	                    b1 = true;
		    	                    break;
		    	                }else
		    	                    t.setContextSize(i);
		    	            }
		    	            t = t.nextP;
		    	        }
		    	        if (b1){
		    	            state = END;
		    	            break;
		    	        }
		    	        
						 b=vn.toElement(VTDNav.FIRST_CHILD);
						 state =  END;
						 if (b == true){
						 do {
							if (currentStep.eval(vn)) {
								if (currentStep.getNextStep() != null){
									//currentStep.position++;
									 state =  FORWARD;
									currentStep = currentStep.getNextStep();
								}
								else {
									 state =  TERMINAL;
									result = vn.getCurrentIndex();
									if ( isUnique(result)){
										return result;
									}
								}
							break;
							}
						} while (vn.toElement(VTDNav.NS));
						if (state == END)
						    vn.toElement(VTDNav.PARENT);
					 }
		    	    } else {
						if (vn.getAtTerminal()==true){
							state = END;
						}else {
						    // compute context size;
						    t = currentStep.p;
			    	        while(t!=null){
			    	            if (t.requireContextSize()){
			    	                int i = computeContextSize(t,vn);
			    	                if (i==0){
			    	                    b1 = true;
			    	                    break;
			    	                }else
			    	                    t.setContextSize(i);
			    	            }
			    	            t = t.nextP;
			    	        }
			    	        // b1 false indicate context size is zero. no need to go any further...
			    	        if (b1){
			    	            state = END;
			    	            break;
			    	        }
			    	        // get textIter
						    TextIter ti = null;
						    if (currentStep.o != null){
						        ti = (TextIter) currentStep.o;
						    } else {
						        ti = new TextIter();
						        currentStep.o = ti;
						    }
						    //select comment, pi or text here
						    selectNodeType(ti);
						    ti.touch(vn);
						    state = END;
						    while((result = ti.getNext())!=-1){
						    	vn.LN = result;
						    	vn.setAtTerminal(true);
						        if (currentStep.evalPredicates(vn)){
									break;
								}
						    }
						    // old code
							//result = vn.getText();
							if (result != -1){
								vn.setAtTerminal(true);
								//currentStep.resetP(vn);
								vn.LN = result;    
								if (currentStep.getNextStep() != null){
								    vn.LN = result;
				   				    state =  FORWARD;
									currentStep = currentStep.getNextStep();
								}
								else {
									//vn.pop();
									 state =  TERMINAL;
									if ( isUnique(result)){
									    vn.LN = result;
										return result;
									}
								}								
							} else{
								//currentStep.set_ft(true);
								currentStep.resetP(vn);
								vn.setAtTerminal(false);
							}
						}		    	        
		    	    }
		    	    break;
		    	case END:
					currentStep =null;
					// reset();
					return -1;
					
		    	case FORWARD:
		    	    if (currentStep.nt.testType < NodeTest.TEXT){
		    	        t = currentStep.p;
		    	        while(t!=null){
		    	            if (t.requireContextSize()){
		    	                int i = computeContextSize(t,vn);
		    	                if (i==0){
		    	                    b1 = true;
		    	                    break;
		    	                }else
		    	                    t.setContextSize(i);
		    	            }
		    	            t = t.nextP;
		    	        }
		    	        if (b1){
		    	            currentStep = currentStep.getPrevStep();
		    	            state = BACKWARD;
		    	            break;
		    	        }
		    	        
		   			 	state =  BACKWARD;
		   			 	forward: if (vn.toElement(VTDNav.FC)) {
							do {
								if (currentStep.eval(vn)) {
									if (currentStep.getNextStep() != null) {
										 state =  FORWARD;
										currentStep = currentStep.getNextStep();
									} else {
										 state =  TERMINAL;
										result = vn.getCurrentIndex();
										if ( isUnique(result))
											return result;
									}
									break forward;
								}
							} while (vn.toElement(VTDNav.NS));
							vn.toElement(VTDNav.P);
							currentStep.resetP(vn);
							currentStep = currentStep.getPrevStep();
						} else {
							//vn.toElement(VTDNav.P);
							currentStep = currentStep.getPrevStep();
						}
			    	}else {
			    	    // predicate at an attribute is not evaled
						if (vn.getAtTerminal() == true){
							state = BACKWARD;
							currentStep = currentStep.getPrevStep();
						}else {
						    // compute context size;
						    t = currentStep.p;
			    	        while(t!=null){
			    	            if (t.requireContextSize()){
			    	                int i = computeContextSize(t,vn);
			    	                if (i==0){
			    	                    b1 = true;
			    	                    break;
			    	                }else
			    	                    t.setContextSize(i);
			    	            }
			    	            t = t.nextP;
			    	        }
			    	        // b1 false indicate context size is zero. no need to go any further...
			    	        if (b1){
			    	            state = BACKWARD;
			    	            break;
			    	        }
			    	        // get textIter
						    TextIter ti = null;
						    if (currentStep.o != null){
						        ti = (TextIter) currentStep.o;
						    } else {
						        ti = new TextIter();
						        currentStep.o = ti;
						    }
						    ti.touch(vn);
						    selectNodeType(ti);
						    //result = ti.getNext();
						    
						    while((result = ti.getNext())!=-1){
						    	vn.setAtTerminal(true);
						    	vn.LN = result;
						        if (currentStep.evalPredicates(vn)){
									break;
								}
						    }						   
						   
			                if (result == -1) {
			                    //currentStep.set_ft(true);
			                    //currentStep.resetP(vn);
			                    vn.setAtTerminal(false);
			                    if (state == FORWARD) {
			                        state = BACKWARD;
			                        currentStep = currentStep.getPrevStep();
			                    }
			                } else {
			                    vn.setAtTerminal(true);
			                    if (currentStep.getNextStep() != null) {
			                        vn.LN = result;
			                        state = FORWARD;
			                        currentStep = currentStep.getNextStep();
			                    } else {
			                        //vn.pop();
			                        state = TERMINAL;
			                        if (isUnique(result)) {
			                            vn.LN = result;
			                            return result;
			                        }
			                    }
			                }
						}				    	        
			    	}

		    	    break;
		    	
		    	case BACKWARD:
					if (currentStep.nt.testType < NodeTest.TEXT) {
						//currentStep = currentStep.getPrevStep();
						b = false;
						while (vn.toElement(VTDNav.NS)) {
							if (currentStep.eval(vn)) {
								b = true;
								break;
							}
						}
						if (b == true) {
							 state =  FORWARD;
							currentStep = currentStep.getNextStep();
						} else if (currentStep.getPrevStep() == null){
							currentStep.resetP(vn);
							vn.toElement(VTDNav.P);
							 state =  END;
						}
						else {
							currentStep.resetP(vn);
							 state =  BACKWARD;
							vn.toElement(VTDNav.P);
							currentStep = currentStep.getPrevStep();
						}
					}else {
						vn.setAtTerminal(false);
						if (currentStep.getPrevStep() == null)
							 state =  END;
						else {
							 state =  BACKWARD;
							 //vn.setAtTerminal(false);
							currentStep = currentStep.getPrevStep();
						}
					}
					break;
		    	    
		    	case TERMINAL:
					if (currentStep.nt.testType < NodeTest.TEXT) {
						while (vn.toElement(VTDNav.NS)) {
							if (currentStep.eval(vn)) {
								// state =  TERMINAL;
								result = vn.getCurrentIndex();
								if ( isUnique(result))
									return result;
							}
						}
						currentStep.resetP(vn);
						if (currentStep.getPrevStep() == null){
							 state =  END;
							 vn.toElement(VTDNav.P);
						}
						else {
							vn.toElement(VTDNav.P);
							
							 state =  BACKWARD;
							currentStep = currentStep.getPrevStep();
						}
					}else {
					    TextIter ti = (TextIter) currentStep.o;
					    while ((result=ti.getNext())!=-1) {
					    	vn.setAtTerminal(true);
					    	vn.LN = result;
					        if (currentStep.evalPredicates(vn)) {
					            if ( isUnique(result))
									return result;
					        }
					    }					    
						currentStep.resetP(vn);
						vn.setAtTerminal(false);
						if (currentStep.getPrevStep() == null)
							 state =  END;
						else {
							 state =  BACKWARD;
							currentStep = currentStep.getPrevStep();
						}
					}
					break;

				default:
					throw new XPathEvalException("unknown state");
		    }
		    return -2;
		}
		
	private final int process_DDFP(VTDNav vn) 
		throws XPathEvalException, NavException {
		AutoPilot ap;
		boolean b = false, b1 = false;
	    Predicate t= null;
		int result;
		
		
		switch(state){
			case START:
			case FORWARD:
			    if (vn.atTerminal){
			        if (state == START)
			            state = END;
			        else {
			            // no need to set_ft to true
			            // no need to resetP
			            state = BACKWARD;
			            currentStep = currentStep.getPrevStep();
			        }
			        break;
			    }
			    
    	        t = currentStep.p;
    	        while(t!=null){
    	            if (t.requireContextSize()){
    	                int i = computeContextSize(t,vn);
    	                if (i==0){
    	                    b1 = true;
    	                    break;
    	                }else
    	                    t.setContextSize(i);
    	            }
    	            t = t.nextP;
    	        }
    	        if (b1){
    	            if (state ==START)
    	                state = END;
    	            else {
    	                currentStep = currentStep.getPrevStep();
    	                state = BACKWARD;
    	            }
    	            break;
    	        }
    	        
    		    String helper = null;
    			if (currentStep.nt.testType == NodeTest.NAMETEST){
    				helper = currentStep.nt.nodeName;
    			} else if (currentStep.nt.testType == NodeTest.NODE){
    				helper = "*";
    			} else
    				throw new XPathEvalException("can't run descendant "
    						+ "following, or following-sibling axis over comment(), pi(), and text()"); 
    			if (currentStep.o == null)
    				currentStep.o = ap = new AutoPilot(vn);
    			else {
    				ap = (AutoPilot) currentStep.o;
    				ap.bind(vn);
    			}
    			if (currentStep.get_ft() == true) {

    				if (currentStep.axis_type == AxisType.DESCENDANT_OR_SELF )
    					if (currentStep.nt.testType == NodeTest.NODE)
    						ap.setSpecial(true);
    					else
    						ap.setSpecial(false);
    				//currentStep.o = ap = new AutoPilot(vn);
    			    if (currentStep.axis_type == AxisType.DESCENDANT_OR_SELF)
    					ap.selectElement(helper);
    				else if (currentStep.axis_type == AxisType.DESCENDANT)
    					ap.selectElement_D(helper);
    				else if (currentStep.axis_type == AxisType.PRECEDING)
    					ap.selectElement_P(helper);
    				else 
    					ap.selectElement_F(helper);
    				currentStep.set_ft(false);
    			}
    			if ( state ==  START)
    				 state =  END;

    			vn.push2(); // not the most efficient. good for now
    			//System.out.println("  --++ push in //");
    			b = false;
    			while(ap.iterate()){
    				if (currentStep.evalPredicates(vn)){
    					b = true;
    					break;
    				}
    			}
    			if (b == false) {
    				vn.pop2();
    				//System.out.println("  --++ pop in //");
    				currentStep.set_ft(true);
    				currentStep.resetP(vn);
    				if ( state ==  FORWARD){
    					state =  BACKWARD;
    					currentStep = currentStep.getPrevStep();							
    				}						
    			} else {
    				if (currentStep.getNextStep() != null){
    					state =  FORWARD;
    					currentStep = currentStep.getNextStep();
    				}
    				else {
    					//vn.pop();
    					state =  TERMINAL;
    					result = vn.getCurrentIndex();
    					if ( isUnique(result))
    						return result;
    				}
    			}
    			break;    	        
			    
			case END:
				currentStep = null;
				// reset();
				return -1;
			    
			case BACKWARD:
				//currentStep = currentStep.getPrevStep();
				ap = (AutoPilot) currentStep.o;
				//vn.push();
				b = false;
				while(ap.iterate()){
					if (currentStep.evalPredicates(vn)){
						b = true;
						break;
					}
				}
				if (b == false) {
					vn.pop2();
					currentStep.set_ft(true);
					currentStep.resetP(vn);
					//System.out.println("  --++ pop in //");
					if (currentStep.getPrevStep() != null) {
						 state =  BACKWARD;
						currentStep = currentStep.getPrevStep();
					} else
						 state =  END;
				} else {
					if (currentStep.getNextStep() != null) {
						//vn.push();
						//System.out.println("  --++ push in //");
						 state =  FORWARD;
						currentStep = currentStep.getNextStep();
					} else {
						 state =  TERMINAL;
						result = vn.getCurrentIndex();
						if ( isUnique(result))
							return result;
					}
				}
				break;
			    
			case TERMINAL:
			    ap = (AutoPilot) currentStep.o;
			    b = false;
			    while (ap.iterate()) {
			        if (currentStep.evalPredicates(vn)) {
			            b = true;
			            break;
			        }
			    }
			    if (b == true) {
			        if (currentStep.evalPredicates(vn)) {
			            result = vn.getCurrentIndex();
			            if (isUnique(result))
			                return result;
			        }
			    } else if (currentStep.getPrevStep() == null) {
			        currentStep.resetP(vn);
			        vn.pop2();
			        state = END;
			    } else {
			        vn.pop2();
			        currentStep.set_ft(true);
			        currentStep.resetP(vn);
			        //System.out.println(" --++ pop in //");
			        state = BACKWARD;
			        //currentStep.ft = true;
			        currentStep = currentStep.getPrevStep();
			    }
            break;

			default:
			    throw new XPathEvalException("unknown state");
        }
	    return -2;
	}
	
	private final int process_parent(VTDNav vn)
	throws XPathEvalException, NavException{
	    boolean b1 = false;
	    Predicate t= null;
	    int result;
		switch ( state) {
			case  START:
			case  FORWARD:
    	        t = currentStep.p;
    	        while(t!=null){
    	            if (t.requireContextSize()){
    	                int i = computeContextSize(t,vn);
    	                if (i==0){
    	                    b1 = true;
    	                    break;
    	                }else
    	                    t.setContextSize(i);
    	            }
    	            t = t.nextP;
    	        }
    	        if (b1){
    	            if (state == FORWARD){
    	                state = BACKWARD;
    	                currentStep = currentStep.getPrevStep();
    	            }else 
    	                state = END;
    	            break;
    	        }
    	        
    			if (vn.getCurrentDepth() == -1) {
    				if ( state ==  START)
    					 state =  END;
    				else {
    					//vn.pop();
    					 state =  BACKWARD;
    					currentStep = currentStep.getPrevStep();
    				}
    			} else {
    				vn.push2();
    				vn.toElement(VTDNav.P); // must return true
    				if (currentStep.eval(vn)){
    				    if (currentStep.getNextStep() != null) {
    					    state =  FORWARD;
    					   currentStep = currentStep.getNextStep();
    				    } else {
    					    state =  TERMINAL;
    					   result = vn.getCurrentIndex();
    						if ( isUnique(result))
    							return result;
    				    }
    				}else{
    					vn.pop2();
    					currentStep.resetP(vn);
    					if ( state ==  START)
    						 state =  END;
    					else {								
    						 state =  BACKWARD;
    						currentStep = currentStep.getPrevStep();
    					}
    				}
    			}

    			break;				
    			
    		case  END:
    			currentStep = null;
    			// reset();
    		    return -1;
    			
    		case  BACKWARD:
    		case  TERMINAL:
    			if (currentStep.getPrevStep() == null) {
    			    vn.pop2();
    				 state =  END;
    				break;
    			}else {
    				vn.pop2();
    				 state =  BACKWARD;
    				currentStep = currentStep.getPrevStep();
    				break;
    			}
    			
    		default:
    			throw new  XPathEvalException("unknown state");
		
		}
	    return -2;
	}
	
	private final int process_ancestor( VTDNav vn)
	throws XPathEvalException, NavException{
	    int result;
	    boolean b = false, b1 = false;
	    //int contextSize;
	    Predicate t= null;
	    
	    switch(state){
	    	case START:
	    		
	    	    t = currentStep.p;
	    	    while (t != null) {
	    	        if (t.requireContextSize()) {
	    	            int i = computeContextSize( t, vn);
	    	            if (i == 0) {
	    	                b1 = true;
	    	                break;
	    	            } else
	    	                t.setContextSize(i);
	    	        }
	    	        t = t.nextP;
	    	    }
	    	    if (b1) {
	    	        state = END;
	    	        break;
	    	    }

	    	    state = END;
	    	    if (vn.getCurrentDepth() != -1) {
	    	        vn.push2();

	    	        while (vn.toElement(VTDNav.P)) {
	    	            if (currentStep.eval(vn)) {
	    	                if (currentStep.getNextStep() != null) {
	    	                    state = FORWARD;
	    	                    currentStep = currentStep.getNextStep();
	    	                    break;
	    	                } else {
	    	                    //vn.pop();
	    	                    state = TERMINAL;
	    	                    result = vn.getCurrentIndex();
	    	                    if (isUnique(result))
	    	                        return result;
	    	                }
	    	            }
	    	        }
	    	        if (state == END) {
	    	            currentStep.resetP(vn);
	    	            vn.pop2();
	    	        }
	    	    }
	    	    break;
    	        
	    	case END:   
				currentStep =null;
				// reset();
			    return -1;
			    
	    	case FORWARD:	    	    
	    	     t = currentStep.p;
	    	     while(t!=null){
	    	        if (t.requireContextSize()){
	    	             int i = computeContextSize(t,vn);
	    	             if (i==0){
	    	                 b1 = true;
	    	                 break;
	    	             }else
	    	                 t.setContextSize(i);
	    	        }
	    	        t = t.nextP;
	    	    }
	    	    if (b1){
	    	        currentStep = currentStep.getPrevStep();
	    	        state = BACKWARD;
	    	        break;
	    	    }
			    state =  BACKWARD;
			   	vn.push2();
					
			   	while(vn.toElement(VTDNav.P)){
			   		if (currentStep.eval(vn)){
			   			if (currentStep.getNextStep() != null){
			   				 state =  FORWARD;
			   				currentStep = currentStep.getNextStep();
			   				break;
			   			}
			   			else {
			   				//vn.pop();
			   				 state =  TERMINAL;
			   				result = vn.getCurrentIndex();
							if ( isUnique(result))
								return result;
			   			}
			   		}							
			   	}
			   	if ( state == BACKWARD){
			   		currentStep.resetP(vn);
					vn.pop2();
			   		currentStep=currentStep.getPrevStep();
			   	}			    
			  	break;
	    	    
	    	case BACKWARD:
				b = false;
				vn.push2();

				while (vn.toElement(VTDNav.P)) {
					if (currentStep.eval(vn)) {
						if (currentStep.getNextStep()!= null) {
							 state =  FORWARD;
							currentStep = currentStep.getNextStep();
							b = true;
							break;
						} else {
							//vn.pop();
							 state =  TERMINAL;
							result = vn.getCurrentIndex();
							if ( isUnique(result))
								return result;
						}
					}
				}
				if (b==false){
					vn.pop2();
					if (currentStep.getPrevStep()!=null) {
						currentStep.resetP(vn);
						state =  BACKWARD;
						currentStep = currentStep.getPrevStep();
					}
					else {
						 state =  END;
					}
				}
				break;
				
	    	case TERMINAL:			
	    	    while (vn.toElement(VTDNav.P)) {
				if (currentStep.eval(vn)) {
					result = vn.getCurrentIndex();
					if ( isUnique(result))
						return result;
				}
			}
			vn.pop2();
			
			if (currentStep.getPrevStep()!=null) {
				currentStep.resetP(vn);
				 state =  BACKWARD;
				currentStep = currentStep.getPrevStep();
			}
			else {
				 state =  END;
			}
			break;
		
		default:
			throw new  XPathEvalException("unknown state");
	    }
	    return -2;
	}
	
	private final int process_ancestor_or_self(VTDNav vn)
	throws XPathEvalException,NavException{
	    boolean b = false, b1 = false;
	    Predicate t= null;
	    int result;
		switch ( state) {
			case  START:
	    	    t = currentStep.p;
	    	    while (t != null) {
	    	        if (t.requireContextSize()) {
	    	            int i = computeContextSize( t, vn);
	    	            if (i == 0) {
	    	                b1 = true;
	    	                break;
	    	            } else
	    	                t.setContextSize(i);
	    	        }
	    	        t = t.nextP;
	    	    }
	    	    if (b1) {
	    	        state = END;
	    	        break;
	    	    }
				state =  END;
				vn.push2();
				
				if (currentStep.get_ft()== true){						
					currentStep.set_ft(false);
					if (currentStep.eval(vn)) {
						if (currentStep.getNextStep() != null) {
							state =  FORWARD;
							currentStep = currentStep.getNextStep();
							break;
						} else {
							//vn.pop();
							state =  TERMINAL;
							if (vn.atTerminal)
							    result = vn.LN;
							else 
							    result = vn.getCurrentIndex();
							if ( isUnique(result))
								return result;
						}
					}
				}
				
					while (vn.toElement(VTDNav.P)) {
						if (currentStep.eval(vn)) {
							if (currentStep.getNextStep() != null) {
								 state =  FORWARD;
								currentStep = currentStep.getNextStep();
								break;
							} else {
								//vn.pop();
								 state =  TERMINAL;
								result = vn.getCurrentIndex();
								if ( isUnique(result))
									return result;
							}
						}
					}
				
				if ( state ==  END) {
					currentStep.resetP(vn);
					vn.pop2();
				}

				break;
				
			case  FORWARD:
	    	     t = currentStep.p;
	    	     while(t!=null){
	    	        if (t.requireContextSize()){
	    	             int i = computeContextSize(t,vn);
	    	             if (i==0){
	    	                 b1 = true;
	    	                 break;
	    	             }else
	    	                 t.setContextSize(i);
	    	        }
	    	        t = t.nextP;
	    	    }
	    	    if (b1){
	    	        currentStep = currentStep.getPrevStep();
	    	        state = BACKWARD;
	    	        break;
	    	    }
				 state =  BACKWARD;
					vn.push2();
					if (currentStep.get_ft() == true) {
						currentStep.set_ft(false);
						
						if (currentStep.eval(vn)) {
							if (currentStep.getNextStep() != null) {
								 state =  FORWARD;
								currentStep = currentStep.getNextStep();
								break;
							} else {
								//vn.pop();
								 state =  TERMINAL;
								 if (vn.atTerminal)
								     result = vn.LN;
								 else 
								     result = vn.getCurrentIndex();
								if ( isUnique(result))
									return result;
							}
						}
					} 
						while (vn.toElement(VTDNav.P)) {
							if (currentStep.eval(vn)) {
								if (currentStep.getNextStep() != null) {
									 state =  FORWARD;
									currentStep = currentStep.getNextStep();
									break;
								} else {
									//vn.pop();
									 state =  TERMINAL;
									result = vn.getCurrentIndex();
									if ( isUnique(result))
										return result;
								}
							}
						}
					
					if ( state ==  BACKWARD) {
						currentStep.resetP(vn);
						currentStep.set_ft(true);
						vn.pop2();
						currentStep = currentStep.getPrevStep();
					}
					break;
					
			case  END:
				currentStep = null;
				// reset();
		    	return -1;
				
			
			case  BACKWARD:
				b = false;
				vn.push2();

				while (vn.toElement(VTDNav.P)) {
					if (currentStep.eval(vn)) {
						if (currentStep.getNextStep() != null) {
							 state =  FORWARD;
							currentStep = currentStep.getNextStep();
							b = true;
							break;
						} else {
							//vn.pop();
							 state =  TERMINAL;
							result = vn.getCurrentIndex();
							if ( isUnique(result))
								return result;
						}
					}
				}
				if (b == false) {
					vn.pop2();
					currentStep.resetP(vn);
					if (currentStep.getPrevStep() != null) {
						currentStep.set_ft(true);
						 state =  BACKWARD;
						currentStep = currentStep.getPrevStep();
					} else {
						 state =  END;
					}
				}
				break;
			
			case  TERMINAL:
				while (vn.toElement(VTDNav.P)) {
					if (currentStep.eval(vn)) {
						result = vn.getCurrentIndex();
						if ( isUnique(result))
							return result;
					}
				}
				vn.pop2();
				currentStep.resetP(vn);
				if (currentStep.getPrevStep()!=null) {
					currentStep.set_ft(true);
					 state =  BACKWARD;
					currentStep = currentStep.getPrevStep();
				}
				else {
					 state =  END;
				}
				break;
				
			
			default:
				throw new  XPathEvalException("unknown state");
		}
	    return -2;
	}
	private final int process_self(VTDNav vn)
		throws XPathEvalException,NavException{
	    boolean b1 = false;
	    Predicate t= null;
	    int result;
		switch( state){
		  case  START:
		  case  FORWARD:
  	        t = currentStep.p;
	        while(t!=null){
	            if (t.requireContextSize()){
	                int i = computeContextSize(t,vn);
	                if (i==0){
	                    b1 = true;
	                    break;
	                }else
	                    t.setContextSize(i);
	            }
	            t = t.nextP;
	        }
	        if (b1){
	            if (state == FORWARD){
	                state = BACKWARD;
	                currentStep = currentStep.getPrevStep();
	            }else 
	                state = END;
	            break;
	        }
		  	if (currentStep.eval(vn)){
		  		if (currentStep.getNextStep()!=null){
		  			 state =  FORWARD;
		  			currentStep = currentStep.getNextStep();
		  		}
		  		else{
		  			 state =  TERMINAL;
		  			 if (vn.atTerminal == true)
		  			     result = vn.LN;
		  			 else 
		  			     result = vn.getCurrentIndex();
					if ( isUnique(result))
						return result;
		  		}
		  	}else {
		  		currentStep.resetP(vn);
		  		if ( state ==  START)
		  			 state =  END;
		  		else 
		  			 state =  BACKWARD;
		  	}
		    break;
		  	
		  case  END:
		  	currentStep = null;
		  	// reset();
		  	return -1;
		  	
		  case  BACKWARD:
		  case  TERMINAL:
		  	if (currentStep.getPrevStep()!=null){
	  			 state =  BACKWARD;
	  			currentStep= currentStep.getPrevStep();
	  		}else{
	  			 state =  END;				  			
	  		}
		  	break;
		  
		  default:
			throw new  XPathEvalException("unknown state");
		}
	    return -2;
	}
	
	
	private final int process_namespace(VTDNav vn)
	throws XPathEvalException,NavException {
	    AutoPilot ap = null;
	    boolean b1 = false;
	    Predicate t= null;
	    int temp;
		switch( state){
		case  START:
		case  FORWARD:
		    
	        t = currentStep.p;
	        while(t!=null){
	            if (t.requireContextSize()){
	                int i = computeContextSize(t,vn);
	                if (i==0){
	                    b1 = true;
	                    break;
	                }else
	                    t.setContextSize(i);
	            }
	            t = t.nextP;
	        }
	        if (b1){
	            if (state == FORWARD){
	                state = BACKWARD;
	                currentStep = currentStep.getPrevStep();
	            }else 
	                state = END;
	            break;
	        }
	        
			if (vn.getAtTerminal()==true){
				if (state ==START)
					state = END;
				else {
					state = BACKWARD;
					currentStep  = currentStep.getPrevStep();
				}
			} else {
				
                if (currentStep.get_ft() == true) {
                    if (currentStep.o == null)
                        currentStep.o = ap = new AutoPilot(vn);
                    else {
                        ap = (AutoPilot) currentStep.o;
                        ap.bind(vn);
                        //ap.set_ft(true);
                    }
                    if (currentStep.nt.testType == NodeTest.NODE)
                    	ap.selectNameSpace("*");
                    else
                    	ap.selectNameSpace(currentStep.nt.nodeName);
                    currentStep.set_ft(false);
                }
                if (state == START)
                    state = END;
                vn.push2();
                //vn.setAtTerminal(true);
                while ((temp = ap.iterateNameSpace()) != -1) {
                    if (currentStep.evalPredicates(vn)) {
                        break;
                    }
                }
                if (temp == -1) {
                	vn.pop2();
                    currentStep.set_ft(true);
                    currentStep.resetP(vn);
                    vn.setAtTerminal(false);
                    if (state == FORWARD) {
                        state = BACKWARD;
                        currentStep = currentStep.getPrevStep();
                    }
                } else {
                	vn.setAtTerminal(true);
                    if (currentStep.getNextStep() != null) {
                        vn.LN = temp;
                        state = FORWARD;
                        currentStep = currentStep.getNextStep();
                    } else {
                        //vn.pop();
                        state = TERMINAL;
                        if (isUnique(temp)) {
                            vn.LN = temp;
                            return temp;
                        }
                    }

                }
            }
			break;
			
		case  END:
			currentStep = null;
			// reset();
	  		return -1;
	  		
		case  BACKWARD:
			ap = (AutoPilot) currentStep.o;
			//vn.push();
			while( (temp = ap.iterateNameSpace()) != -1){
				if (currentStep.evalPredicates(vn)){
					break;
				}							
			}
			if (temp == -1) {
				vn.pop2();
				currentStep.set_ft(true);
				currentStep.resetP(vn);
				vn.setAtTerminal(false);
				if (currentStep.getPrevStep() != null) {
					state =  BACKWARD;
					currentStep = currentStep.getPrevStep();
				} else
					state =  END;
			} else {
				if (currentStep.getNextStep() != null) {
					state =  FORWARD;
					currentStep = currentStep.getNextStep();
				} else {
					state =  TERMINAL;
					if ( isUnique(temp)){
					    vn.LN = temp;
						return temp;
					}
				}
			}
			break;
			
		case  TERMINAL:
			ap = (AutoPilot) currentStep.o;
			while( (temp = ap.iterateNameSpace()) != -1){
				if (currentStep.evalPredicates(vn)){
					break;
				}							
			}
			if (temp != -1) 
				if (isUnique(temp)){
				    vn.LN = temp;
					return temp;
				}
			vn.setAtTerminal(false);
			currentStep.resetP(vn);
			if (currentStep.getPrevStep() == null) {
				currentStep.set_ft(true);
				vn.pop2();
				 state =  END;
			} else {
				 state =  BACKWARD;
				 vn.pop2();
				currentStep.set_ft(true);
				currentStep = currentStep.getPrevStep();
			}
			
			break;					
		
		default:
			throw new  XPathEvalException("unknown state");
	}
	    return -2;
	}
	
	private final int process_following_sibling(VTDNav vn)
	throws XPathEvalException,NavException{
	    boolean b = false, b1 = false;
	    Predicate t= null;
	    int result;
		switch( state){
		  case  START:
		  case  FORWARD:

  	        t = currentStep.p;
	        while(t!=null){
	            if (t.requireContextSize()){
	                int i = computeContextSize(t,vn);
	                if (i==0){
	                    b1 = true;
	                    break;
	                }else
	                    t.setContextSize(i);
	            }
	            t = t.nextP;
	        }
	        if (b1){
	            if (state == FORWARD){
	                state = BACKWARD;
	                currentStep = currentStep.getPrevStep();
	            }else 
	                state = END;
	            break;
	        }
		  	if ( state ==  START)
		  		 state =  END;
		  	else
		  		 state =  BACKWARD;
		  	vn.push2();
		  	while (vn.toElement(VTDNav.NS)){
		  		if (currentStep.eval(vn)){
		  			if (currentStep.getNextStep()!=null){
		  				 state =  FORWARD;
		  				currentStep = currentStep.getNextStep();
		  				break;
		  			} else {
		  				 state =  TERMINAL;
		  				result = vn.getCurrentIndex();
						if ( isUnique(result))
							return result;
		  			}
		  		}
		  	}
		  	
		  	if ( state ==  END){
		  		currentStep.resetP(vn);
		  		vn.pop2();
		  	}else if ( state ==  BACKWARD){
		  		currentStep.resetP(vn);
		  		vn.pop2();
		  		currentStep = currentStep.getPrevStep();				  		
		  	}
		    break;
		  	 
		  case  END:
		  	currentStep = null;
		  	// reset();
		  	return -1;
		  	
		  case  BACKWARD:
		  	while (vn.toElement(VTDNav.NS)){
		  		if (currentStep.eval(vn)){
		  			if (currentStep.getNextStep()!=null){
		  				 state =  FORWARD;
		  				currentStep = currentStep.getNextStep();
		  				b = true;
		  				break;
		  			} else {
		  				 state =  TERMINAL;
		  				result = vn.getCurrentIndex();
						if ( isUnique(result))
							return result;
		  			}
		  		}
		  	}
		    if (b==false){
		    	vn.pop2();
		    	currentStep.resetP(vn);
		    	if (currentStep.getPrevStep()==null){
		    		 state =  END;
		    	}else{
		    		 state =  BACKWARD;
		    		currentStep = currentStep.getPrevStep();
		    	}
		    }
		  	break;
		  
		  case  TERMINAL:
		  	while (vn.toElement(VTDNav.NS)){
		  		if (currentStep.eval(vn)){
		  			// state =  TERMINAL;
		  			result = vn.getCurrentIndex();
					if ( isUnique(result))
						return result;
		  		}
		  	}
		  	vn.pop2();
		  	currentStep.resetP(vn);
		  	if(currentStep.getPrevStep()!=null){
		  		currentStep = currentStep.getPrevStep();
		  		 state =  BACKWARD;
		  	}else{
		  		 state =  END;
		  	}
		  	break;

		  default:
			throw new  XPathEvalException("unknown state");
		}
	    return -2;
	}
	
	private final int process_preceding_sibling(VTDNav vn)
	throws XPathEvalException,NavException {
	    boolean b = false, b1 = false;
	    Predicate t= null;
	    int result;
	    switch(state){
		  case  START:
		  case  FORWARD:
  	        t = currentStep.p;
	        while(t!=null){
	            if (t.requireContextSize()){
	                int i = computeContextSize(t,vn);
	                if (i==0){
	                    b1 = true;
	                    break;
	                }else
	                    t.setContextSize(i);
	            }
	            t = t.nextP;
	        }
	        if (b1){
	            if (state == FORWARD){
	                state = BACKWARD;
	                currentStep = currentStep.getPrevStep();
	            }else 
	                state = END;
	            break;
	        }  
		  	if ( state ==  START)
		  		 state =  END;
		  	else
		  		 state =  BACKWARD;
		  	vn.push2();
		  	while (vn.toElement(VTDNav.PS)){
		  		if (currentStep.eval(vn)){
		  			if (currentStep.getNextStep()!=null){
		  				 state =  FORWARD;
		  				currentStep = currentStep.getNextStep();
		  				break;
		  			} else {
		  				 state =  TERMINAL;
		  				result = vn.getCurrentIndex();
						if ( isUnique(result))
							return result;
		  			}
		  		}
		  	}
		  	
		  	if ( state ==  END){
		  		currentStep.resetP(vn);
		  		vn.pop2();
		  	}else if ( state ==  BACKWARD){
		  		currentStep.resetP(vn);
		  		vn.pop2();
		  		currentStep = currentStep.getPrevStep();				  		
		  	}
		  	 break;
		  	 
		  case  END:
		  	currentStep = null;
		  	// reset();
		  	return -1;
		  
		  case  BACKWARD:
		  	while (vn.toElement(VTDNav.PS)){
		  		if (currentStep.eval(vn)){
		  			if (currentStep.getNextStep()!=null){
		  				 state =  FORWARD;
		  				currentStep = currentStep.getNextStep();
		  				b = true;
		  				break;
		  			} else {
		  				 state =  TERMINAL;
		  				result = vn.getCurrentIndex();
						if ( isUnique(result))
							return result;
		  			}
		  		}
		  	}
		    if (b==false){
		    	vn.pop2();
		    	currentStep.resetP(vn);
		    	if (currentStep.getPrevStep()==null){
		    		 state =  END;
		    	}else{
		    		 state =  BACKWARD;
		    		currentStep = currentStep.getPrevStep();
		    	}
		    }
		  	break;
		  
		  case  TERMINAL:
		  	while (vn.toElement(VTDNav.PS)){
		  		if (currentStep.eval(vn)){
		  			// state =  TERMINAL;
		  			result = vn.getCurrentIndex();
					if ( isUnique(result))
						return result;
		  		}
		  	}
		  	vn.pop2();
		  	if(currentStep.getPrevStep()!=null){
		  		currentStep = currentStep.getPrevStep();
		  		 state =  BACKWARD;
		  	}else{
		  		 state =  END;
		  	}
		  	break;
		  
		  default:
			throw new  XPathEvalException("unknown state");
		}
	    return -2;
	}
	
	private final int process_attribute(VTDNav vn)
	throws XPathEvalException,NavException {
	    AutoPilot ap = null;
	    boolean b1 = false;
	    Predicate t= null;
	    int temp;
		switch( state){
		case  START:
		case  FORWARD:
		    
	        t = currentStep.p;
	        while(t!=null){
	            if (t.requireContextSize()){
	                int i = computeContextSize(t,vn);
	                if (i==0){
	                    b1 = true;
	                    break;
	                }else
	                    t.setContextSize(i);
	            }
	            t = t.nextP;
	        }
	        if (b1){
	            if (state == FORWARD){
	                state = BACKWARD;
	                currentStep = currentStep.getPrevStep();
	            }else 
	                state = END;
	            break;
	        }
	        
			if (vn.getAtTerminal()==true){
				if (state ==START)
					state = END;
				else {
					state = BACKWARD;
					currentStep  = currentStep.getPrevStep();
				}
			} else {
                if (currentStep.get_ft() == true) {
                    if (currentStep.o == null)
                        currentStep.o = ap = new AutoPilot(vn);
                    else {
                        ap = (AutoPilot) currentStep.o;
                        ap.bind(vn);
                        //ap.set_ft(true);
                    }
                    if (currentStep.nt.testType == NodeTest.NODE)
                    	ap.selectAttr("*");
                    else if (currentStep.nt.localName != null)
                        ap.selectAttrNS(currentStep.nt.URL,
                                currentStep.nt.localName);
                    else
                        ap.selectAttr(currentStep.nt.nodeName);
                    currentStep.set_ft(false);
                }
                if (state == START)
                    state = END;
                vn.setAtTerminal(true);
                while ((temp = ap.iterateAttr2()) != -1) {
                    if (currentStep.evalPredicates(vn)) {
                        break;
                    }
                }
                if (temp == -1) {
                    currentStep.set_ft(true);
                    currentStep.resetP(vn);
                    vn.setAtTerminal(false);
                    if (state == FORWARD) {
                        state = BACKWARD;
                        currentStep = currentStep.getPrevStep();
                    }
                } else {

                    if (currentStep.getNextStep() != null) {
                        vn.LN = temp;
                        state = FORWARD;
                        currentStep = currentStep.getNextStep();
                    } else {
                        //vn.pop();
                        state = TERMINAL;
                        if (isUnique(temp)) {
                            vn.LN = temp;
                            return temp;
                        }
                    }

                }
            }
			break;
			
		case  END:
			currentStep = null;
			// reset();
	  		return -1;
	  		
		case  BACKWARD:
			ap = (AutoPilot) currentStep.o;
			//vn.push();
			while( (temp = ap.iterateAttr2()) != -1){
				if (currentStep.evalPredicates(vn)){
					break;
				}							
			}
			if (temp == -1) {
				currentStep.set_ft(true);
				currentStep.resetP(vn);
				vn.setAtTerminal(false);
				if (currentStep.getPrevStep() != null) {
					state =  BACKWARD;
					currentStep = currentStep.getPrevStep();
				} else
					state =  END;
			} else {
				if (currentStep.getNextStep() != null) {
					state =  FORWARD;
					currentStep = currentStep.getNextStep();
				} else {
					state =  TERMINAL;
					if ( isUnique(temp)){
					    vn.LN = temp;
						return temp;
					}
				}
			}
			break;
			
		case  TERMINAL:
			ap = (AutoPilot) currentStep.o;
			while( (temp = ap.iterateAttr2()) != -1){
				if (currentStep.evalPredicates(vn)){
					break;
				}							
			}
			if (temp != -1) 
				if (isUnique(temp)){
				    vn.LN = temp;
					return temp;
				}
			vn.setAtTerminal(false);
			currentStep.resetP(vn);
			if (currentStep.getPrevStep() == null) {
				currentStep.set_ft(true);
				 state =  END;
			} else {
				 state =  BACKWARD;
				currentStep.set_ft(true);
				currentStep = currentStep.getPrevStep();
			}
			
			break;					
		
		default:
			throw new  XPathEvalException("unknown state");
	}
	    return -2;
	}
	
	public int evalNodeSet(VTDNav vn) 
    	throws NavException,XPathEvalException{
        int result;
		if (currentStep == null) {
			if ( pathType ==  ABSOLUTE_PATH){
				vn.toElement(VTDNav.ROOT);
				vn.toElement(VTDNav.PARENT);
			}
			currentStep =  s;
			if (currentStep == null){
				if (  state ==  START){
					 state =  END;
					return 0;
				}
				else{
					return -1;
				}
			}
		}
		
		while (true) {
			switch (currentStep.axis_type) {

			case AxisType.CHILD:
			    if ( (result = process_child(vn))!=-2)
				   return result;
			    break;
			case AxisType.DESCENDANT_OR_SELF:
			case AxisType.DESCENDANT:
			case AxisType.PRECEDING:								
			case AxisType.FOLLOWING:
			    if ((result = process_DDFP(vn))!= -2)
			        return result;
			    break;
			case AxisType.PARENT:
			    if ((result = process_parent(vn))!= -2)
			        return result;
			    break;
			case AxisType.ANCESTOR:
			    if ((result = process_ancestor(vn))!= -2)
			        return result;
			    break;
			case AxisType.ANCESTOR_OR_SELF:
			    if ((result = process_ancestor_or_self(vn))!= -2)
			        return result;
			    break;
			case AxisType.SELF:
			    if ((result = process_self(vn))!= -2)
			        return result;
			    break;
			case AxisType.FOLLOWING_SIBLING:
			    if ((result = process_following_sibling(vn))!= -2)
			        return result;
			    break;
			case AxisType.PRECEDING_SIBLING:
			    if ((result = process_preceding_sibling(vn))!= -2)
			        return result;
			    break;
			case AxisType.ATTRIBUTE:
			    if ((result = process_attribute(vn))!= -2)
			        return result;
			    break;
			default:
				if ((result = process_namespace(vn))!= -2)
			        return result;
			}
		}
        
    }
    
	public boolean isString(){
	    return false;
	}
	
	public boolean isBoolean(){
	    return false;
	}
	
	// to support computer context size 
	// needs to add 
	public boolean requireContextSize(){
	    return false;
	}
	
	// 
	public void setContextSize(int size){	    
	}
	
	public int computeContextSize(Predicate p, VTDNav vn)
		throws NavException,XPathEvalException{
	    
	    boolean b = false;
	    //Predicate tp = null;
	    int i = 0;
	    AutoPilot ap = (AutoPilot)currentStep.o;
	    switch(currentStep.axis_type){
	    	case AxisType.CHILD:
	    	    if (currentStep.nt.testType < NodeTest.TEXT){
	    	    b = vn.toElement(VTDNav.FIRST_CHILD);
	    		if (b) {
	    		    do {
	    		        if (currentStep.eval(vn, p)) {
                        	i++;
	    		        }
	    		    } while (vn.toElement(VTDNav.NS));	    		    
	    		    vn.toElement(VTDNav.PARENT);
	    		    currentStep.resetP(vn,p);
	    		    return i;
	    		} else
	    		    return 0;
	    	    }else {	    
	    	        TextIter ti = new TextIter();
	    	        ti.touch(vn);
	    	        selectNodeType(ti);
	    	        int result = -1;
	    	        while((result=ti.getNext())!=-1){
	    	        	vn.setAtTerminal(true);
	    	        	vn.LN = result;
	    	            if (currentStep.evalPredicates(vn,p)){
	    	                i++;
	    	            }
	    	        }
	    	        vn.atTerminal = false;
	    	        currentStep.resetP(vn,p);
	    	        return i;
	    	    }
	    		   
			case AxisType.DESCENDANT_OR_SELF:
			case AxisType.DESCENDANT:
			case AxisType.PRECEDING:								
			case AxisType.FOLLOWING:
			    
			    String helper = null;
				if (currentStep.nt.testType == NodeTest.NODE){
				    helper = "*";
				}else if (currentStep.nt.testType == NodeTest.NAMETEST){
    				helper = currentStep.nt.nodeName;
    			}else
    				throw new XPathEvalException("can't run descendant "
    						+ "following, or following-sibling axis over comment(), pi(), and text()");
				if (ap==null)
					ap = new AutoPilot(vn);
				else
					ap.bind(vn);
				if (currentStep.axis_type == AxisType.DESCENDANT_OR_SELF )
					if (currentStep.nt.testType == NodeTest.NODE)
						ap.setSpecial(true);
					else
						ap.setSpecial(false);
				//currentStep.o = ap = new AutoPilot(vn);
			    if (currentStep.axis_type == AxisType.DESCENDANT_OR_SELF)
			        if (currentStep.nt.localName!=null)
			            ap.selectElementNS(currentStep.nt.URL,currentStep.nt.localName);
			        else 
			            ap.selectElement(helper);
				else if (currentStep.axis_type == AxisType.DESCENDANT)
				    if (currentStep.nt.localName!=null)
				        ap.selectElementNS_D(currentStep.nt.URL,currentStep.nt.localName);
				    else 
				        ap.selectElement_D(helper);
				else if (currentStep.axis_type == AxisType.PRECEDING)
				    if (currentStep.nt.localName!=null)
				        ap.selectElementNS_P(currentStep.nt.URL,currentStep.nt.localName);
				    else 
				        ap.selectElement_P(helper);
				else 
				    if (currentStep.nt.localName!=null)
				        ap.selectElementNS_F(currentStep.nt.URL,currentStep.nt.localName);
				    else 
				        ap.selectElement_F(helper);
			    vn.push2();
    			while(ap.iterate()){
    				if (currentStep.evalPredicates(vn,p)){
    					i++;
    				}
    			}
    			vn.pop2();
    			currentStep.resetP(vn,p);
    			currentStep.o = ap;
    			return i;
			  
			case AxisType.PARENT:
			    vn.push2();
				i = 0;
				if (vn.toElement(VTDNav.PARENT)){
				    if (currentStep.eval(vn,p)){
				        i++;
				    }
				}			    
				vn.pop2();
				currentStep.resetP(vn,p);
				currentStep.o = ap;
				return i;
				
			case AxisType.ANCESTOR:
			    vn.push2();
				i = 0;
				while (vn.toElement(VTDNav.PARENT)) {
				    if (currentStep.eval(vn, p)) {
                    	i++;
    		        }
				}				
				vn.pop2();
				currentStep.resetP(vn,p);
				currentStep.o = ap;
				return i;
				
			case AxisType.ANCESTOR_OR_SELF:
			    vn.push2();
				i = 0;
				do {
				    if (currentStep.eval(vn, p)) {
                    	i++;
    		        }
				}while(vn.toElement(VTDNav.PARENT));
				vn.pop2();
				currentStep.resetP(vn,p);
				currentStep.o = ap;
				return i;
				
			case AxisType.SELF:
			    i = 0;
				if (vn.toElement(VTDNav.PARENT)){
				    if (currentStep.eval(vn,p)){
				        i++;
				    }
				}			    
				currentStep.resetP(vn,p);
				currentStep.o = ap;
				return i;
			    
			case AxisType.FOLLOWING_SIBLING:
			    vn.push2();
				while(vn.toElement(VTDNav.NEXT_SIBLING)){
				    if (currentStep.eval(vn,p)){
				        i++;
				    }
				}			    
			    vn.pop2();
				currentStep.resetP(vn,p);
				currentStep.o = ap;
				return i;
			    
			case AxisType.PRECEDING_SIBLING:
			    vn.push2();
				while(vn.toElement(VTDNav.PREV_SIBLING)){
				    if (currentStep.eval(vn,p)){
				        i++;
				    }
				}			    
				vn.pop2();
				currentStep.resetP(vn,p);
				currentStep.o = ap;
				return i;
				
			case AxisType.ATTRIBUTE:
				if (ap==null)
					ap = new AutoPilot(vn);
				else
					ap.bind(vn);
				if (currentStep.nt.testType == NodeTest.NODE)
					ap.selectAttr("*");
				else if (currentStep.nt.localName!=null)
				    ap.selectAttrNS(currentStep.nt.URL,
			            currentStep.nt.localName);
				else 
				    ap.selectAttr(currentStep.nt.nodeName);
				i = 0;
				while(ap.iterateAttr2()!=-1){
				    if (currentStep.evalPredicates(vn,p)){
				        i++;
				    }
				}
          		currentStep.resetP(vn,p);
          		currentStep.o = ap;
				return i;
			    
			case AxisType.NAMESPACE:
				if (ap==null)
					ap = new AutoPilot(vn);
				else
					ap.bind(vn);
				if (currentStep.nt.testType == NodeTest.NODE)
                	ap.selectNameSpace("*");
                else
                	ap.selectNameSpace(currentStep.nt.nodeName);
				i=0;
				vn.push2();
				while(ap.iterateNameSpace()!=-1){
				    if (currentStep.evalPredicates(vn,p)){
				        i++;
				    }
				}	    
				vn.pop2();
				currentStep.resetP(vn,p);
				currentStep.o = ap;
				return i;
	    	default:
	    	    throw new XPathEvalException("axis not supported");
	    }
	    //return 8;
	}
	
	public void setPosition(int pos){
	    
	}
	
	public int adjust(int n) {
	    int i;
        if (pathType == RELATIVE_PATH) {
            i = Math.min(intHash.determineHashWidth(n),6); // hash width 64 
        } else {
            i = intHash.determineHashWidth(n);
        }
        if (ih!=null && i== ih.e)
        {}
        else 
            ih = new intHash(i);
        return i;
	}
	
	private void selectNodeType(TextIter ti){
		if (currentStep.nt.testType == NodeTest.TEXT )
			ti.selectText();
		else if (currentStep.nt.testType == NodeTest.COMMENT )
			ti.selectComment();
		else if (currentStep.nt.testType == NodeTest.PI0 )
			ti.selectPI0();
		else {
			ti.selectPI1(currentStep.nt.nodeName);
		}
		
	}
	
}

