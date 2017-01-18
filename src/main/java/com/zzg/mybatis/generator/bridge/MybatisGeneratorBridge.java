package com.zzg.mybatis.generator.bridge;

import com.zzg.mybatis.generator.model.DatabaseConfig;
import com.zzg.mybatis.generator.model.DbType;
import com.zzg.mybatis.generator.model.GeneratorConfig;
import com.zzg.mybatis.generator.plugins.DbRemarksCommentGenerator;
import com.zzg.mybatis.generator.util.DbUtil;
import org.mybatis.generator.api.MyBatisGenerator;
import org.mybatis.generator.api.ProgressCallback;
import org.mybatis.generator.api.ShellCallback;
import org.mybatis.generator.config.*;
import org.mybatis.generator.internal.DefaultShellCallback;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The bridge between GUI and the mybatis generator. All the operation to
 * mybatis generator should proceed through this class
 * <p>
 * Created by Owen on 6/30/16.
 */
public class MybatisGeneratorBridge {

	private GeneratorConfig generatorConfig;

	private DatabaseConfig selectedDatabaseConfig;

	private ProgressCallback progressCallback;

	private List<IgnoredColumn> ignoredColumns;

	private List<ColumnOverride> columnOverrides;

	public MybatisGeneratorBridge() {
		init();
	}

	private void init() {
		Configuration config = new Configuration();
		Context context = new Context(ModelType.CONDITIONAL);
		config.addContext(context);
	}

	public void setGeneratorConfig(GeneratorConfig generatorConfig) {
		this.generatorConfig = generatorConfig;
	}

	public void setDatabaseConfig(DatabaseConfig databaseConfig) {
		this.selectedDatabaseConfig = databaseConfig;
	}

	public void generate() throws Exception {
		Configuration config = new Configuration();
		config.addClasspathEntry(generatorConfig.getConnectorJarPath());
		Context context = new Context(ModelType.CONDITIONAL);
		context.setId("myid");
		context.setTargetRuntime("MyBatis3");
		config.addContext(context);
		//处理配置项
		configureComment(context);
		configureTable(context);
		configureJdbc(context);
		configureJavaModel(context);
		configureXmlMapper(context);
		configureJavaMapper(context);
		//加载插件
		loadPlugins(context);

		List<String> warnings = new ArrayList<>();
		Set<String> fullyqualifiedTables = new HashSet<>();
		Set<String> contexts = new HashSet<>();
		ShellCallback shellCallback = new DefaultShellCallback(true); // override=true
		MyBatisGenerator myBatisGenerator = new MyBatisGenerator(config,
				shellCallback, warnings);
		myBatisGenerator.generate(progressCallback, contexts,
				fullyqualifiedTables);
	}

	/**
	 * 加载插件
	 * @param context
	 */
	private void loadPlugins(Context context) {
		// limit/offset插件
		if (generatorConfig.isOffsetLimit()) {
			PluginConfiguration pluginConfiguration = new PluginConfiguration();
			pluginConfiguration.addProperty("type",
					"com.zzg.mybatis.generator.plugins.MySQLLimitPlugin");
			pluginConfiguration
					.setConfigurationType("com.zzg.mybatis.generator.plugins.MySQLLimitPlugin");
			context.addPluginConfiguration(pluginConfiguration);
		}

	}

	private void configureJavaMapper(Context context) {
		// DAO
		JavaClientGeneratorConfiguration daoConfig = new JavaClientGeneratorConfiguration();
		daoConfig.setConfigurationType("XMLMAPPER");
		daoConfig.setTargetPackage(generatorConfig.getDaoPackage());
		daoConfig.setTargetProject(generatorConfig.getProjectFolder() + "/"
				+ generatorConfig.getDaoTargetFolder());
		context.setJavaClientGeneratorConfiguration(daoConfig);

	}

	private void configureXmlMapper(Context context) {
		// Mapper config
		SqlMapGeneratorConfiguration mapperConfig = new SqlMapGeneratorConfiguration();
		mapperConfig.setTargetPackage(generatorConfig.getMappingXMLPackage());
		mapperConfig.setTargetProject(generatorConfig.getProjectFolder() + "/"
				+ generatorConfig.getMappingXMLTargetFolder());
		context.setSqlMapGeneratorConfiguration(mapperConfig);
	}

	private void configureJavaModel(Context context) {
		// java model
		JavaModelGeneratorConfiguration javaModelConfig = new JavaModelGeneratorConfiguration();
		javaModelConfig.setTargetPackage(generatorConfig.getModelPackage());
		javaModelConfig.setTargetProject(generatorConfig.getProjectFolder()
				+ "/" + generatorConfig.getModelPackageTargetFolder());
		context.setJavaModelGeneratorConfiguration(javaModelConfig);
	}

	private void configureJdbc(Context context) {
		JDBCConnectionConfiguration jdbcConfig = new JDBCConnectionConfiguration();
		jdbcConfig.setDriverClass(DbType.valueOf(
				selectedDatabaseConfig.getDbType()).getDriverClass());
		jdbcConfig.setConnectionURL(DbUtil
				.getConnectionUrlWithSchema(selectedDatabaseConfig));
		jdbcConfig.setUserId(selectedDatabaseConfig.getUsername());
		jdbcConfig.setPassword(selectedDatabaseConfig.getPassword());
		context.setJdbcConnectionConfiguration(jdbcConfig);

	}

	private void configureTable(Context context) {
		// Table config
		TableConfiguration tableConfig = new TableConfiguration(context);
		tableConfig.setTableName(generatorConfig.getTableName());
		tableConfig.setDomainObjectName(generatorConfig.getDomainObjectName());
		// example是否生成
		tableConfig.setCountByExampleStatementEnabled(generatorConfig
				.isExample());
		tableConfig.setDeleteByExampleStatementEnabled(generatorConfig
				.isExample());
		tableConfig.setSelectByExampleStatementEnabled(generatorConfig
				.isExample());
		tableConfig.setUpdateByExampleStatementEnabled(generatorConfig
				.isExample());
		// add ignore columns
		if (ignoredColumns != null) {
			ignoredColumns.stream().forEach(ignoredColumn -> {
				tableConfig.addIgnoredColumn(ignoredColumn);
			});
		}
		if (columnOverrides != null) {
			columnOverrides.stream().forEach(columnOverride -> {
				tableConfig.addColumnOverride(columnOverride);
			});
		}
		context.addTableConfiguration(tableConfig);

	}

	/**
	 * 配置Comment
	 * 
	 * @param context
	 */
	private void configureComment(Context context) {
		// Comment
		CommentGeneratorConfiguration commentConfig = new CommentGeneratorConfiguration();
		commentConfig.setConfigurationType(DbRemarksCommentGenerator.class
				.getName());
		commentConfig.addProperty("javaFileEncoding", "UTF-8");
		if (generatorConfig.isComment()) {
			commentConfig.addProperty("columnRemarks", "true");
		}
		context.setCommentGeneratorConfiguration(commentConfig);

	}

	public void setProgressCallback(ProgressCallback progressCallback) {
		this.progressCallback = progressCallback;
	}

	public void setIgnoredColumns(List<IgnoredColumn> ignoredColumns) {
		this.ignoredColumns = ignoredColumns;
	}

	public void setColumnOverrides(List<ColumnOverride> columnOverrides) {
		this.columnOverrides = columnOverrides;
	}
}
