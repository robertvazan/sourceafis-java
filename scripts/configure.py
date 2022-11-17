# This script generates and updates project configuration files.

# We are assuming that project-config is available in sibling directory.
# Checkout from https://github.com/robertvazan/project-config
import pathlib
project_directory = lambda: pathlib.Path(__file__).parent.parent
config_directory = lambda: project_directory().parent/'project-config'
exec((config_directory()/'src'/'java.py').read_text())

project_script_path = __file__
repository_name = lambda: 'sourceafis-java'
pretty_name = lambda: 'SourceAFIS for Java'
pom_subgroup = lambda: 'sourceafis'
pom_artifact = lambda: 'sourceafis'
pom_name = lambda: 'SourceAFIS'
pom_description = lambda: 'Fingerprint recognition engine that takes a pair of human fingerprint images and returns their similarity score. Supports efficient 1:N search.'
inception_year = lambda: 2009
homepage = lambda: website() + 'java'
jdk_version = lambda: 11
stagean_annotations = lambda: True

def dependencies():
    use('com.machinezoo.fingerprintio:fingerprintio:1.3.0')
    use_fastutil()
    use_commons_io()
    use_gson()
    use_jackson_cbor()
    use('com.github.mhshams:jnbis:2.1.1')
    use_junit()
    use_hamcrest()

javadoc_links = lambda: [
    *standard_javadoc_links(),
    'https://noexception.machinezoo.com/javadocs/core/',
    'https://fingerprintio.machinezoo.com/javadoc/'
]

def documentation_links():
    yield 'SourceAFIS for Java', homepage()
    yield 'Javadoc', javadoc_home()
    yield 'SourceAFIS overview', 'https://sourceafis.machinezoo.com/'
    yield 'Algorithm', 'https://sourceafis.machinezoo.com/algorithm'

generate()
