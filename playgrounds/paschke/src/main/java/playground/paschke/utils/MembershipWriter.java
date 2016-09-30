package playground.paschke.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;

public class MembershipWriter extends MatsimXmlWriter {
	protected Map<Id<Person>, Map<String, List<String>>> memberships;

	public MembershipWriter(Map<Id<Person>, Map<String, List<String>>> memberships) {
		this.memberships = memberships;
	}

	public void writeFile(final String filename) {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeDoctype("memberships", "CSMembership.dtd");

		this.writeStartTag("memberships", Collections.<Tuple<String, String>>emptyList());

		Iterator<Entry<Id<Person>, Map<String, List<String>>>> iterator = memberships.entrySet().iterator();
		while (iterator.hasNext()) {
			Entry<Id<Person>, Map<String, List<String>>> personMemberships = iterator.next();

			this.writeStartTag("person", Arrays.asList(createTuple("id", personMemberships.getKey().toString())));

			Iterator<Entry<String, List<String>>> companyIterator = personMemberships.getValue().entrySet().iterator();
			while (companyIterator.hasNext()) {
				Entry<String, List<String>> company = companyIterator.next();
				this.writeStartTag("company", Arrays.asList(createTuple("id", company.getKey())));
				for (String type : company.getValue()) {
					this.writeStartTag("carsharing", Arrays.asList(createTuple("name", type)), true);
				}
				this.writeEndTag("company");
			}
			this.writeEndTag("person");
		}

		this.writeEndTag("memberships");
		this.close();
	}
}
