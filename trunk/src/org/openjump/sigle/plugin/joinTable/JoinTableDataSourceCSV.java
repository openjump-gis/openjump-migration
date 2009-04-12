/**
 * @author Olivier BEDEL
 * Laboratoire RESO UMR 6590 CNRS
 * Bassin Versant du Jaudy-Guindy-Bizien
 * 26 oct. 2004
 * 
 */
package org.openjump.sigle.plugin.joinTable;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StreamTokenizer;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.StringTokenizer;

import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jump.I18N;
import com.vividsolutions.jump.feature.AttributeType;

/**
 * @author Olivier BEDEL
 * Laboratoire RESO UMR 6590 CNRS
 * Bassin Versant du Jaudy-Guindy-Bizien
 * 26 oct. 2004
 * license Licence CeCILL http://www.cecill.info/
 * 
 */
public class JoinTableDataSourceCSV implements JoinTableDataSource {
	
	private ArrayList fieldNames = null;
	private ArrayList fieldTypes = null;
	private String filePath = null;
	private int fieldCount = 0;
	private String DEFAULT_DELEM = "\t";
	private String DELIMITATEURS = ";|\t"; //expression reguliere de qualification des delimitateurs de champs : tab ou ;
	
	public JoinTableDataSourceCSV(String filePath) {
		this.filePath = filePath;
		fieldNames = new ArrayList();
		readHeader();
	}
	
	public ArrayList getFieldNames() {
		return fieldNames;
	}
	
	public ArrayList getFieldTypes() {
		return fieldTypes;
	}
		

	
	
	public Hashtable buildTable (int keyIndex) {
			FileReader fileReader;
			BufferedReader bufferedReader;
		
			Hashtable table = new Hashtable();	
			int nl=1;
			int nbCol;
			String s, line;
			String[] valeurs, val, aux;
			
			fieldTypes = new ArrayList();
			try {
				fileReader = new FileReader(filePath);
				FileInputStream fis = new FileInputStream(filePath);
				bufferedReader = new BufferedReader(new InputStreamReader(fis));
						
				 
				try {
					// passage de la premiere ligne
					line= bufferedReader.readLine();
					nl++;
					line= bufferedReader.readLine();
					nl++;
					if (line==null)
						throw (new Exception(I18N.get("org.openjump.sigle.plugin.joinTable.Empty_file")));
					nl=1;
					while (line!=null) {
						// [OBEDEL]on encadre la chaine de la ligne pour que la fonction split 
						// prenne en compte les champs vides en debut et en fin de ligne 
						line = " " + DEFAULT_DELEM + line  + DEFAULT_DELEM + " ";
						aux = line.split(DELIMITATEURS);
						valeurs = new String[aux.length-2];
						nbCol = valeurs.length;
						
						// verification de la coherence du nombre de colonnes de l'entete et de la ligne 
						if ((nbCol)!=fieldCount)
						/*	// cas du delimitateur en fin de ligne
							if (nbCol==fieldCount-1 && line.substring(nbCol-1).matches(DELIMITATEURS)) {
								val = new String[fieldCount];
								val[fieldCount-1] = "" ;
								for (int i=0; i<fieldCount-1; i++)
									val[i] = valeurs[i];
								valeurs= val;  
							}
							else */
								throw (new Exception(I18N.get("org.openjump.sigle.plugin.joinTable.Field_problem_at_line") + nl));
						
						// on elimine les valeurs que l'on a rajoute en debut et en fin de ligne
						for (int i=0;i<valeurs.length;i++)
							valeurs[i]=aux[i+1];
						
						// analyse de la valeur de chaque colonne
						for (int i=0; i<nbCol; i++) {
							s = (String) valeurs[i]; 
							// mise a jour du type du champ
							if ((i+1)>fieldTypes.size())
								fieldTypes.add(i,typeOfString(s));
							else {
								AttributeType newFieldType = typeOfString(s);
								AttributeType fieldType = (AttributeType) fieldTypes.get(i);
								if 	(newFieldType!=fieldType)
								{
									if (newFieldType == AttributeType.STRING)
										fieldTypes.set(i,newFieldType);
									else if (fieldType!= AttributeType.STRING && newFieldType==AttributeType.DOUBLE)
										fieldTypes.set(i,newFieldType);
								}
							}
						}
						line= bufferedReader.readLine(); 
						nl++;
						//enregistrement de la ligne dans la table
						table.put(valeurs[keyIndex], valeurs);
					}
				}
				catch(Exception e) {
					String msg = I18N.get("org.openjump.sigle.plugin.joinTable.Error_while_reading_file") + filePath +" (" + e.getMessage() + ").";
					throw (new ParseException(msg));
				}		
				finally {
					bufferedReader.close(); 
					fileReader.close();
				}
			}
			catch (Exception e) {
				throw new IllegalStateException(e.getMessage());
			}
//	  obedel debug		
/*			AttributeType t;
			System.out.println(fieldTypes.size());
			for (int i=0; i<fieldTypes.size(); i++) {
				t = (AttributeType) fieldTypes.get(i); 
				System.out.println(t.toString() + " ");
	
		}*/	

		return table;
		}

	private void readHeader() {
		FileReader fileReader;
		BufferedReader bufferedReader;
		String s,firstLine, champs[];
		try {
			fileReader = new FileReader(filePath);
			FileInputStream fis = new FileInputStream(filePath);
			// do not manage UTF-16 encoded files
			//bufferedReader = new BufferedReader(new InputStreamReader(fis,"UTF-8"));
			bufferedReader = new BufferedReader(new InputStreamReader(fis));
			
			try {
				
				// boucle de chargement des nom de champs
				firstLine = bufferedReader.readLine();
				if (firstLine==null)
					throw (new Exception(I18N.get("org.openjump.sigle.plugin.joinTable.Empty_file")));
				champs = firstLine.split(DELIMITATEURS);	
				
				// boucle de qualification unique des noms de colonne				
				for (int k=0; k<champs.length; k++) { 
					String suffixe, saux;
					int j,i;
					boolean nomUnique;
					
					s = champs[k];
					nomUnique = false;
					suffixe = "";
					j = 0;
					i = 1;
					while (!nomUnique) {
						nomUnique = true;
						j = 0;
						while (j<fieldNames.size()){
							saux = (String) fieldNames.get(j);
							nomUnique = nomUnique && !(s+suffixe).equalsIgnoreCase(saux); 
							j++;
						}
						if (!nomUnique) {
							suffixe = String.valueOf(i); 
							i++;
						}
					}
					
					fieldNames.add(s + suffixe);
					
				}
				
				//mise a jour de fieldCount
				fieldCount = fieldNames.size(); 
			}
			catch(Exception e) {
				String msg = I18N.get("org.openjump.sigle.plugin.joinTable.Error_while_reading_fields_in_file") + filePath +"(" + e.getMessage() + ").";
				throw (new ParseException(msg));
			}		
			finally {
				bufferedReader.close(); 
				fileReader.close();
			}
		}
		catch (Exception e) {
			throw new IllegalStateException(e.getMessage());
		}
	}


	
	
	private AttributeType typeOfString(String s)
	{
		char firstChar, lastChar;
		AttributeType res = AttributeType.STRING;
		if (s.length()==0)
			res = AttributeType.INTEGER;
		else {
			firstChar = s.charAt(0);
			lastChar = s.charAt(s.length()-1);
			if ((firstChar=='0' && s.length()==1) || (firstChar>'0' && firstChar<='9') || firstChar == '-' || firstChar=='.' || firstChar==',') {
				if (s.indexOf('.')== -1 && s.indexOf(',')== -1)
					if (s.length()< 10)
						res = AttributeType.INTEGER;
					else
						res = AttributeType.DOUBLE;	// integers with more than 10 digits are processed as strings
				else
					res = AttributeType.DOUBLE;
			}
			else
				res = AttributeType.STRING;
		}
		return res; 
	}
}
