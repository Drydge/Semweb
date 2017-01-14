import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BBC{
    static String PREFIX = "PREFIX dctypes: <http://purl.org/dc/dcmitype/>\n" +
            "PREFIX rdf:   <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
            "PREFIX owl:   <http://www.w3.org/2002/07/owl#>\n" +
            "PREFIX xsd:   <http://www.w3.org/2001/XMLSchema#>\n" +
            "PREFIX wo:    <http://purl.org/ontology/wo/>\n" +
            "PREFIX skos:  <http://www.w3.org/2004/02/skos/core#>\n" +
            "PREFIX rdfs:  <http://www.w3.org/2000/01/rdf-schema#>\n" +
            "PREFIX foaf:  <http://xmlns.com/foaf/0.1/>\n" +
            "PREFIX dc:    <http://purl.org/dc/terms/>\n" +
            "PREFIX po:    <http://purl.org/ontology/po/>\n";
    public static void main(String[] args) throws URISyntaxException, IOException {

        Model model = ModelFactory.createDefaultModel();
        String url = args[0];
        String rdf="";
        URL oracle = new URL(url+".rdf");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(oracle.openStream()));

        String inputLine;
        while ((inputLine = in.readLine()) != null)
            rdf+=inputLine+"\n";
        in.close();

        rdf = rdf.replace("<dc:description>","<dc:description rdf:parseType='Literal'>");
        InputStream is = new ByteArrayInputStream( rdf.getBytes() );
        model.read(is, url,"RDF/XML");
        model.write(System.out,"Turtle");

        String query="SELECT ?name WHERE {<"+url+"#species> rdfs:label ?name.}";
        String specieName = Query(model,query).get("?name").toString();


        query="SELECT ?desc WHERE {<"+url+"#species> dc:description ?desc.}";
        String specieDesc = Query(model,query).get("?desc").toString();
        specieDesc =specieDesc.replace("^^http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral","");


        query="SELECT ?sci WHERE {<"+url+"#species> wo:name ?uriname. ?uriname rdfs:label ?sci.}";
        String specieSciName = Query(model,query).get("?sci").toString();


        query="SELECT ?url WHERE {<"+url+"#species> foaf:depiction ?url.}";
        String image = Query(model,query).get("?url").toString();


        List<String> classifications = Arrays.asList("kingdom","phylum","class","order","family","genus");
        String htmlclass ="";
        for (String inlist:classifications) {
            query="SELECT ?href ?name WHERE {" +
                    "<"+url+"#species> wo:name ?uriname. ?uriname wo:"+inlist+"Name ?name.\n}";
            QuerySolution sol = Query(model, query);
            String name=sol.get("?name").toString();
            htmlclass+="\t<li>"+inlist+": <a href=''>"+name+"</a></li>\n";

        }






        query="SELECT ?common WHERE {<"+url+"#species> wo:name ?uriname. ?uriname wo:commonName ?common.}";
        ArrayList<QuerySolution> solList = QuerySolList(model, query);
        ArrayList<String> CommonNames= new ArrayList<String>();
        String htmlcommon="";
        for (QuerySolution q: solList) {
            String CommonName = q.get("?common").toString();
            CommonNames.add(CommonName);
            htmlcommon+= "\t<li>"+CommonName+"</li>\n";
        }

        String HTML = "\n" +
                "<!DOCTYPE html>\n" +
                "<html>\n" +
                "  <head>\n" +
                "    <title>"+specieName+"</title>  <!-- Extracted from the rdfs:label of the wo:Species -->\n" +
                "    <meta http-equiv='Content-Type' content='text/html; charset=UTF-8' />\n" +
                "    <style>\n" +
                "      .img {\n" +
                "\tfloat: right;\n" +
                "      }\n" +
                "      .content {\n" +
                "\tfloat: left;\n" +
                "      }\n" +
                "      .main {\n" +
                "\twidth: 974px;\n" +
                "\tmargin: 0 auto;\n" +
                "\tpadding: 0px;\n" +
                "\tbackground: transparent;\n" +
                "\tcolor: white;\n" +
                "\tfont-family: sans-serif;\n" +
                "\tposition: relative;\n" +
                "      }\n" +
                "      body {\n" +
                "\tbackground-color: rgb(26,26,26);\n" +
                "\tbackground-image: url('http://static.bbci.co.uk/naturelibrary/3.1.37/css/2/f/banners/2/fur_banner.jpg');\n" +
                "\tbackground-repeat: no-repeat;\n" +
                "\tbackground-position: 50% 0px;\n" +
                "\tbackground-attachment: scroll;\n" +
                "      }\n" +
                "      a {\n" +
                "\tcolor: rgb(240,140,75);\n" +
                "      }\n" +
                "    </style>\n" +
                "  </head>\n" +
                "  <body>\n" +
                "  <div class='main'>\n" +
                "    <div class='img'>\n" +
                "      <img alt='Human' src='"+image+"'/> <!-- alt is the rdfs:label of the wo:Species, src is its foaf:depiction -->\n" +
                "    </div>\n" +
                "    <h1><a href='file:///nature/species/"+specieName+"#name'>"+specieName+"</a></h1> <!-- rdfs:label of the wo:Species and the link is the IRI of the wo:Species -->\n" +
                "    <p class='content'>"+specieDesc+"</p>  <!-- Extracted from the dc:description of the wo:Species -->\n" +
                "    <div>\n" +
                "      <p>Scientific name: <a href='file:///nature/species/"+specieName+"#name'>"+specieSciName+"</a></p> <!-- link to the wo:name of the wo:Species -->\n" +
                "      <p>Common names:</p> <!-- they are extracted from the wo:commonName of the wo:name of the wo:Species -->\n" +
                "      <ul>\n" +
                htmlcommon+
                "      </ul>\n" +
                "      <p>Classification:</p>\n" +
                "      <ul>\n" +htmlclass+"</ul>\n" +
                "    </div>\n" +
                "  </div>\n" +
                "  </body>\n" +
                "</html>";
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(specieName+".html")));
// normalement si le fichier n'existe pas, il est crée à la racine du projet
            writer.write(HTML);

            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    public static ArrayList<QuerySolution> QuerySolList(Model model, String queryString){
        Query query = QueryFactory.create(PREFIX+queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        try {
            ResultSet results = qexec.execSelect() ;
            ArrayList<QuerySolution> Sol_List = new ArrayList<QuerySolution>();
            for ( ; results.hasNext() ; )
            {
                QuerySolution soln = results.nextSolution() ;
                Sol_List.add(soln);
            }
            return Sol_List;
        } finally { qexec.close() ;}

    }
    public static QuerySolution Query(Model model, String queryString){
        Query query = QueryFactory.create(PREFIX+queryString) ;
        QueryExecution qexec = QueryExecutionFactory.create(query, model) ;
        try {
            ResultSet results = qexec.execSelect() ;
            return results.nextSolution();

        } finally { qexec.close() ;}

    }

}

