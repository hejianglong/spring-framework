package org.springframework.tests.sample.beans.test;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

public class StudentConversionService implements Converter<String, StudentService> {

	@Override
	public StudentService convert(String source) {
		if (StringUtils.hasLength(source)) {
			String[] sources = source.split("#");

			StudentService studentService = new StudentService();
			studentService.setAge(Integer.parseInt(sources[0]));
			studentService.setName(sources[1]);
			return studentService;
		}
		return null;
	}
}
