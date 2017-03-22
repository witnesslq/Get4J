package com.bytegriffin.conf;

import java.io.File;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.io.SAXReader;

import com.bytegriffin.util.StringUtil;

/**
 * xml文件工具类
 */
public final class XmlHelper {

	private static final Logger logger = LogManager.getLogger(XmlHelper.class);

	/**
	 * 用xsd文件来验证xml文件
	 * @param xmlpath
	 * @param xsdpath
	 */
	public static void validate(String xml_file, String xsd_file) {
		if (StringUtil.isNullOrBlank(xsd_file)) {
			logger.warn("配置文件 [" + xsd_file + "] 不存在。");
			return;
		}
		try {
			SchemaFactory schemaFactory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
			File schemaFile = new File(xsd_file);
			Schema schema = schemaFactory.newSchema(schemaFile);
			Validator validator = schema.newValidator();
			Source source = new StreamSource(xml_file);
			validator.validate(source);
		} catch (Exception ex) {
			logger.error("配置文件 [" + xml_file + "] 格式化出错。", ex);
			System.exit(1);
		}
		logger.info("xml配置文件 [" + xml_file + "] 验证成功。");
	}

	/**
	 * SAX加载xml文件
	 * @param filePath
	 * @return
	 */
	public static Document loadXML(String filePath) {
		try {
			SAXReader saxReader = new SAXReader();
			Document doc = saxReader.read(filePath);
			return doc;
		} catch (Exception e) {
			logger.error("xml file " + filePath + " loading success.");
		}
		return null;
	}

	public static void main(String... args) {

		System.out.println("asdfasdfasdf");
		logger.info("ddddddddddd");
	}

}
