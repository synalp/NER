BEGIN {
    label = "NO";
    flag = "noend";
}
{
for (i=1;i<=NF;i++) 
    if ($i != "<p>" && $i != "</p>" && $i != "<td>" && $i != "</td>" && $i != "<tr>" && $i != "</tr>" && $i != "<table>" && $i != "</table>") {
	if ($i == "." || $i == "?" || $i == "!") {
	    printf ("%s\tNO\n\n",$i);
	}
	else {
            str = $i;
	    if (substr(str,1,1) == "<") { #NE begin
		if (index(str,ne) > 0) { #the requested NE begin
		    label = ne "B";
		    flag = "noend";
		}
		sub(/<[a-zA-Z0-9_-]+>/,"",str);
		#print "str="str;
	    }
	    if (substr(str,length(str),1) == ">") { #NE end
		sub(/<\/[a-zA-Z0-9_-]+>/,"",str);
		flag = "end";
	    }
	    print str "\t" label;
	    if (index(label,ne) > 0 && flag == "noend") { #modify label from NEB -> NEI
		label = ne "I";
	    } else {
		label = "NO";
	    }
	}
    }
}
